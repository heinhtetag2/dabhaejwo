package com.dabhaejwo.global.llm;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 호출 시점 단가 조회 포트. 구현은 domain/pricing 에 있다.
 * global 이 domain 타입에 의존하지 않도록 포트를 여기 둔다.
 */
public interface ModelPriceLookup {

    /**
     * {@code at} 시점에 유효한 단가. 없으면 {@code MODEL_PRICE_NOT_FOUND}.
     * 과거 단가를 조회할 수 있어야 재계산·검증이 가능하다.
     */
    ResolvedPrice resolve(LlmProviderName provider, String model, OffsetDateTime at);

    /**
     * @param modelPriceId 어느 단가 행으로 계산했는지 ai_usage 에 남긴다
     * @param outputPer1m  임베딩 모델은 null
     */
    record ResolvedPrice(Long modelPriceId, BigDecimal inputPer1m, BigDecimal outputPer1m) {

        private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000L);

        /** 원가(원). 소수 4자리까지 유지한다 — 반올림하면 누적 오차가 난다. */
        public BigDecimal costOf(int inputTokens, int outputTokens) {
            BigDecimal cost = inputPer1m
                    .multiply(BigDecimal.valueOf(inputTokens))
                    .divide(MILLION, 6, java.math.RoundingMode.HALF_UP);
            if (outputPer1m != null && outputTokens > 0) {
                cost = cost.add(outputPer1m
                        .multiply(BigDecimal.valueOf(outputTokens))
                        .divide(MILLION, 6, java.math.RoundingMode.HALF_UP));
            }
            return cost.setScale(4, java.math.RoundingMode.HALF_UP);
        }
    }
}
