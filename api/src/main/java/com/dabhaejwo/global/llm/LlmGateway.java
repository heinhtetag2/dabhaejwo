package com.dabhaejwo.global.llm;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.BotScope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>모든 LLM 호출의 유일한 경로.</b>
 *
 * <p>서비스가 {@link LlmProvider} 를 직접 주입받아 호출하면 그 호출은 ai_usage 에 남지 않는다.
 * 원가 데이터는 사후에 복원할 수 없으므로 그런 구멍은 영구적이다.
 * 여기서만 적재하기 때문에 누락이 구조적으로 불가능해진다.
 *
 * <p>원가는 <b>호출 시점 단가</b>로 계산해 확정 저장한다. 단가표를 나중에 고쳐도
 * 과거 ai_usage 는 바뀌지 않는다 — 지난달 정산이 틀어지면 안 되기 때문이다.
 */
@Service
public class LlmGateway {

    private final Map<LlmProviderName, LlmProvider> providers = new EnumMap<>(LlmProviderName.class);
    private final ModelPriceLookup priceLookup;
    private final UsageRecorder usageRecorder;

    public LlmGateway(List<LlmProvider> discovered,
                      ModelPriceLookup priceLookup,
                      UsageRecorder usageRecorder) {
        for (LlmProvider p : discovered) {
            this.providers.put(p.name(), p);
        }
        this.priceLookup = priceLookup;
        this.usageRecorder = usageRecorder;
    }

    /**
     * 답변 생성. {@code purpose} 를 필수 인자로 받는 이유는, 용도를 나누지 않으면
     * 같은 모델이 답변용인지 요약용인지 구분되지 않아 절감 지점을 찾을 수 없기 때문이다.
     */
    public GenerateResult generate(BotScope scope,
                                   UsagePurpose purpose,
                                   LlmProviderName providerName,
                                   GenerateRequest request,
                                   UUID conversationId) {
        LlmProvider provider = require(providerName);
        GenerateResult result = provider.generate(request);
        recordUsage(scope, purpose, providerName, result.model(),
                result.inputTokens(), result.outputTokens(), conversationId);
        return result;
    }

    public EmbedResult embed(BotScope scope,
                             UsagePurpose purpose,
                             LlmProviderName providerName,
                             List<String> texts,
                             String model) {
        LlmProvider provider = require(providerName);
        EmbedResult result = provider.embed(texts, model);
        recordUsage(scope, purpose, providerName, result.model(),
                result.inputTokens(), 0, null);
        return result;
    }

    private void recordUsage(BotScope scope,
                             UsagePurpose purpose,
                             LlmProviderName providerName,
                             String model,
                             int inputTokens,
                             int outputTokens,
                             UUID conversationId) {
        OffsetDateTime now = OffsetDateTime.now();
        ModelPriceLookup.ResolvedPrice price = priceLookup.resolve(providerName, model, now);
        BigDecimal cost = price.costOf(inputTokens, outputTokens);
        usageRecorder.record(scope, purpose, providerName, model,
                price.modelPriceId(), inputTokens, outputTokens, cost, conversationId);
    }

    private LlmProvider require(LlmProviderName name) {
        LlmProvider provider = providers.get(name);
        if (provider == null || !provider.available()) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_UNAVAILABLE,
                    name + " 공급사가 설정되어 있지 않습니다");
        }
        return provider;
    }
}
