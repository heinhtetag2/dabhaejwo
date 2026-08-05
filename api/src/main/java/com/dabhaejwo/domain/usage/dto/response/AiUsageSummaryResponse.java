package com.dabhaejwo.domain.usage.dto.response;

import java.math.BigDecimal;

/**
 * AI 사용량 지표 4종. api-contracts.md §6.
 *
 * @param costPerConvKrw         대화 한 건당 평균 원가. 오늘 대화가 없으면 {@code null} 이다 —
 *                               0원으로 보이면 "공짜로 돌고 있다"로 읽힌다
 * @param monthProjectedCostKrw  경과일 평균 × 그 달의 일수. <b>추정치다.</b> 화면이 그렇게 밝힌다
 */
public record AiUsageSummaryResponse(
        long todayTokensIn,
        long todayTokensOut,
        BigDecimal todayCostKrw,
        BigDecimal costPerConvKrw,
        BigDecimal monthCostKrw,
        BigDecimal monthProjectedCostKrw) {
}
