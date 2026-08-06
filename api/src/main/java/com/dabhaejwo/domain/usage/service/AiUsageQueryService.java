package com.dabhaejwo.domain.usage.service;

import com.dabhaejwo.domain.conversation.repository.ConversationRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.usage.dto.response.AiUsageSummaryResponse;
import com.dabhaejwo.domain.usage.dto.response.DailyCostResponse;
import com.dabhaejwo.domain.usage.dto.response.ModelUsageResponse;
import com.dabhaejwo.domain.usage.dto.response.TopTenantUsageResponse;
import com.dabhaejwo.domain.usage.repository.AiUsageRepository;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.UsagePurpose;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 사용량 분해. 원가가 <b>어디서 얼마나</b> 발생하는지 본다.
 *
 * <p>이 화면은 {@code ai_usage} 를 직접 집계한다 — 일 집계 테이블에는 모델·용도 구분이
 * 없기 때문이다. 대신 월 단위로 범위를 묶는다 (admin-console-plan.md §6.1).
 *
 * <p><b>답변 파이프라인이 없어 지금은 전부 0 이다.</b> 그래도 배관을 먼저 까는 이유는
 * 원가 데이터가 서비스 오픈과 동시에 쌓이기 시작해야 하기 때문이다 — 나중에 붙이면
 * 과거 데이터가 없어 어느 업체가 언제부터 적자였는지 영영 알 수 없다 (§11 1단계).
 */
@Service
public class AiUsageQueryService {

    /** 시각은 전부 UTC 기준으로 자른다. 화면이 어느 타임존인지와 무관하게 같은 숫자가 나와야 한다. */
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    private final AiUsageRepository aiUsageRepository;
    private final ConversationRepository conversationRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;

    public AiUsageQueryService(AiUsageRepository aiUsageRepository,
                               ConversationRepository conversationRepository,
                               TenantRepository tenantRepository,
                               PlanRepository planRepository) {
        this.aiUsageRepository = aiUsageRepository;
        this.conversationRepository = conversationRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public AiUsageSummaryResponse summary() {
        LocalDate today = LocalDate.now(ZONE);
        OffsetDateTime dayStart = today.atStartOfDay().atOffset(ZONE);
        OffsetDateTime dayEnd = dayStart.plusDays(1);
        OffsetDateTime monthStart = today.withDayOfMonth(1).atStartOfDay().atOffset(ZONE);

        AiUsageRepository.Totals todayTotals = aiUsageRepository.totalsBetween(dayStart, dayEnd);
        AiUsageRepository.Totals monthTotals = aiUsageRepository.totalsBetween(monthStart, dayEnd);

        long todayConvCount = conversationRepository
                .countByStartedAtGreaterThanEqualAndStartedAtLessThan(dayStart, dayEnd);

        return new AiUsageSummaryResponse(
                todayTotals.getTokensIn(),
                todayTotals.getTokensOut(),
                todayTotals.getCostKrw(),
                // 대화가 없으면 대화당 원가는 정의되지 않는다. 0원으로 보이면 공짜로 읽힌다.
                todayConvCount == 0 ? null
                        : todayTotals.getCostKrw().divide(BigDecimal.valueOf(todayConvCount), 2, RoundingMode.HALF_UP),
                monthTotals.getCostKrw(),
                project(monthTotals.getCostKrw(), today));
    }

    /**
     * 월말 예상. 경과일 평균 × 그 달의 일수다.
     *
     * <p>정교한 예측이 아니다 — 스파이크성인 문서 학습 원가가 초반에 몰리면 과대 추정된다.
     * 그래도 두는 이유는 "이대로 가면 얼마"라는 자릿수 감각이 조치의 계기가 되기 때문이다.
     */
    private BigDecimal project(BigDecimal monthCost, LocalDate today) {
        int elapsedDays = today.getDayOfMonth();
        int daysInMonth = YearMonth.from(today).lengthOfMonth();
        return monthCost
                .divide(BigDecimal.valueOf(elapsedDays), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(daysInMonth))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 최근 N일 용도별 원가. 데이터가 없는 날도 0 으로 채워 내려보낸다 —
     * 빠진 날이 있으면 막대 간격이 어긋나 추이를 잘못 읽는다.
     */
    @Transactional(readOnly = true)
    public List<DailyCostResponse> daily(int days) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate from = today.minusDays(days - 1L);
        OffsetDateTime fromAt = from.atStartOfDay().atOffset(ZONE);
        OffsetDateTime toAt = today.plusDays(1).atStartOfDay().atOffset(ZONE);

        Map<LocalDate, Map<UsagePurpose, BigDecimal>> byDay = new HashMap<>();
        for (AiUsageRepository.DailyPurposeCost row : aiUsageRepository.aggregateDailyByPurpose(fromAt, toAt)) {
            byDay.computeIfAbsent(row.getDay(), key -> new EnumMap<>(UsagePurpose.class))
                    .put(UsagePurpose.valueOf(row.getPurpose()), row.getCostKrw());
        }

        List<DailyCostResponse> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate day = from.plusDays(i);
            Map<UsagePurpose, BigDecimal> costs = byDay.getOrDefault(day, Map.of());
            result.add(new DailyCostResponse(
                    day,
                    costs.getOrDefault(UsagePurpose.ANSWER, BigDecimal.ZERO),
                    costs.getOrDefault(UsagePurpose.EMBED_DOC, BigDecimal.ZERO),
                    costs.getOrDefault(UsagePurpose.EMBED_QUERY, BigDecimal.ZERO),
                    costs.getOrDefault(UsagePurpose.ETC, BigDecimal.ZERO)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ModelUsageResponse> byModel(YearMonth month) {
        OffsetDateTime from = month.atDay(1).atStartOfDay().atOffset(ZONE);
        OffsetDateTime to = month.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZONE);

        List<AiUsageRepository.ModelUsage> rows = aiUsageRepository.aggregateByModel(from, to);
        BigDecimal total = rows.stream()
                .map(AiUsageRepository.ModelUsage::getCostKrw)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> new ModelUsageResponse(
                        LlmProviderName.valueOf(row.getProvider()),
                        row.getModel(),
                        UsagePurpose.valueOf(row.getPurpose()),
                        row.getCallCount(),
                        row.getInputTokens(),
                        row.getOutputTokens(),
                        row.getCostKrw(),
                        sharePercent(row.getCostKrw(), total)))
                .toList();
    }

    private int sharePercent(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    @Transactional(readOnly = true)
    public List<TopTenantUsageResponse> topTenants(int limit) {
        LocalDate today = LocalDate.now(ZONE);
        OffsetDateTime from = today.withDayOfMonth(1).atStartOfDay().atOffset(ZONE);
        OffsetDateTime to = today.plusDays(1).atStartOfDay().atOffset(ZONE);

        List<AiUsageRepository.TenantCost> rows =
                aiUsageRepository.aggregateByTenant(from, to, PageRequest.of(0, Math.max(limit, 1)));

        Map<UUID, Tenant> tenants = tenantRepository
                .findAllById(rows.stream().map(AiUsageRepository.TenantCost::getTenantId).toList())
                .stream()
                .collect(Collectors.toMap(Tenant::getId, tenant -> tenant));
        Map<UUID, Plan> plans = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, plan -> plan));

        return rows.stream().map(row -> {
            Tenant tenant = tenants.get(row.getTenantId());
            Plan plan = tenant == null ? null : plans.get(tenant.getPlanId());
            // 대화당 원가를 내려면 **원가가 나간 대화**로 나눠야 한다. 열기만 한 방문을
            // 분모에 넣으면 대화당 원가가 실제보다 싸 보인다.
            long convCount = conversationRepository
                    .countAnsweredByTenantIdBetween(row.getTenantId(), from, to);
            return new TopTenantUsageResponse(
                    new TopTenantUsageResponse.TenantRef(
                            row.getTenantId(),
                            tenant == null ? "(삭제된 업체)" : tenant.getName()),
                    plan == null ? null : plan.getName(),
                    row.getTokens(),
                    row.getCostKrw(),
                    convCount == 0 ? null
                            : row.getCostKrw().divide(BigDecimal.valueOf(convCount), 2, RoundingMode.HALF_UP));
        }).toList();
    }
}
