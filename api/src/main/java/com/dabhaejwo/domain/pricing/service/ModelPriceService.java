package com.dabhaejwo.domain.pricing.service;

import com.dabhaejwo.domain.pricing.dto.request.ModelPriceCreateRequest;
import com.dabhaejwo.domain.pricing.dto.response.ModelPriceResponse;
import com.dabhaejwo.domain.pricing.entity.ModelPrice;
import com.dabhaejwo.domain.pricing.repository.ModelPriceRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.ModelPriceLookup;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ModelPriceService implements ModelPriceLookup {

    /**
     * 대화당 원가 추정에 쓰는 상수. <b>추정치의 근거이지 측정값이 아니다.</b>
     *
     * <p>답변 파이프라인이 붙으면 실제 토큰 분포로 다시 잡아야 한다. 그때까지는
     * 요금제 가격을 정할 때 자릿수를 가늠하는 용도다 (docs/IMPROVEMENTS.md 등록).
     */
    private static final int TOKENS_PER_CHUNK = 400;
    /** 시스템 프롬프트 + 대화 맥락. */
    private static final int PROMPT_OVERHEAD_TOKENS = 600;
    /** {@code cost_guards.answer_max_length} 기본값(400자)에 대응하는 대략적 출력 토큰. */
    private static final int ANSWER_OUTPUT_TOKENS = 300;

    /** 단가 이력 화면이 한 번에 보여주는 최대 행 수. 무제한 조회를 하지 않는다. */
    private static final int HISTORY_LIMIT = 100;

    private final ModelPriceRepository repository;
    private final AuditLogService auditLogService;

    public ModelPriceService(ModelPriceRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public ResolvedPrice resolve(LlmProviderName provider, String model, OffsetDateTime at) {
        ModelPrice price = repository
                .findFirstByProviderAndModelAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(provider, model, at)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MODEL_PRICE_NOT_FOUND,
                        provider + "/" + model + " 의 단가가 등록되어 있지 않습니다"));
        // 단가가 없으면 조용히 0원으로 처리하지 않는다 — 원가 0인 호출이 원장에 쌓이면
        // 적자를 발견하지 못한다. 명시적으로 실패시켜 단가 등록을 강제한다.
        return new ResolvedPrice(price.getId(), price.getInputPer1m(), price.getOutputPer1m());
    }

    /**
     * 대화 한 건의 예상 원가. 요금제별 모델 배정 화면이 "이 값의 3배는 받아야 한다"의
     * 근거로 쓴다 (admin-console-plan.md §4.7).
     *
     * <p>저장하지 않고 매번 계산한다 — 단가가 바뀌면 따라 움직여야 하는 값이다.
     * 단가가 등록돼 있지 않으면 {@code null} 이다. 0 으로 내려보내면 "공짜"로 읽힌다.
     */
    @Transactional(readOnly = true)
    public BigDecimal estimateCostPerConversation(LlmProviderName provider, String model, int chunkCount) {
        try {
            ResolvedPrice price = resolve(provider, model, OffsetDateTime.now());
            int inputTokens = PROMPT_OVERHEAD_TOKENS + chunkCount * TOKENS_PER_CHUNK;
            return price.costOf(inputTokens, ANSWER_OUTPUT_TOKENS);
        } catch (BusinessException e) {
            return null;
        }
    }

    /**
     * 단가 이력 전체. 최근 것이 위다.
     *
     * <p>{@code current} 는 그 (공급사, 모델) 조합에서 <b>지금 적용 중인 행</b>인지다.
     * 서버가 판단하는 이유는, 프론트가 {@code effectiveFrom} 을 비교하다 보면
     * 미래 예약분과 현재분의 경계에서 어긋나기 때문이다.
     */
    @Transactional(readOnly = true)
    public List<ModelPriceResponse> list() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ModelPrice> prices = repository.findAllByOrderByProviderAscModelAscEffectiveFromDesc(
                Limit.of(HISTORY_LIMIT));

        // (공급사, 모델)별로 "지금 시점에 유효한 가장 최근 행" 하나만 current 다.
        Set<Long> currentIds = prices.stream()
                .filter(price -> !price.getEffectiveFrom().isAfter(now))
                .collect(Collectors.toMap(
                        price -> price.getProvider() + "/" + price.getModel(),
                        price -> price,
                        (first, second) ->
                                first.getEffectiveFrom().isAfter(second.getEffectiveFrom()) ? first : second))
                .values().stream()
                .map(ModelPrice::getId)
                .collect(Collectors.toSet());

        return prices.stream()
                .map(price -> ModelPriceResponse.of(price, currentIds.contains(price.getId())))
                .toList();
    }

    /**
     * 새 단가 등록. <b>행을 추가만 한다.</b>
     *
     * <p>수정·삭제 메서드가 없는 것이 설계다. 기존 행을 고치면 그 단가로 이미 계산된 과거
     * {@code ai_usage} 의 근거가 사라지고, 언제부터 적자였는지 추적할 수 없게 된다.
     */
    @Transactional
    public ModelPriceResponse register(ModelPriceCreateRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();

        if (request.purposeKind() == ModelPrice.PurposeKind.GENERATE && request.outputPer1m() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "답변 생성 모델은 출력 단가가 필요합니다");
        }

        ModelPrice saved = repository.save(ModelPrice.register(
                request.provider(), request.model(), request.purposeKind(),
                request.inputPer1m(), request.outputPer1m(),
                request.effectiveFrom() == null ? OffsetDateTime.now() : request.effectiveFrom(),
                request.note()));

        auditLogService.record(operator.operatorId(), AuditAction.MODEL_PRICE_WRITE, null,
                request.reason(),
                Map.of("provider", saved.getProvider().name(), "model", saved.getModel(),
                        "inputPer1m", saved.getInputPer1m().toPlainString()));

        return ModelPriceResponse.of(saved, !saved.getEffectiveFrom().isAfter(OffsetDateTime.now()));
    }
}
