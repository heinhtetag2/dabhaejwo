package com.dabhaejwo.domain.billing.dto.response;

import java.math.BigDecimal;

/**
 * 이번 달 정산 요약 (api-contracts.md §6-1).
 *
 * <p><b>매출을 한 단어로 부르지 않는다.</b> 아래 넷은 전부 다른 값이고 서로 대체할 수 없다:
 * <ul>
 *   <li>{@code mrrKrw} — 앞으로 매달 얼마가 들어오나 (계약 기준 정가)
 *   <li>{@code billedKrw} — 얼마를 청구했나
 *   <li>{@code collectedKrw} — <b>얼마를 실제로 받았나 (진짜 매출)</b>
 *   <li>{@code outstandingKrw} — 오늘 쫓아가야 할 돈은 얼마인가
 * </ul>
 *
 * @param marginPercent 수납액이 0이면 null. 0% 로 내리면 "남는 게 없다"로 읽히는데
 *                      사실은 받은 돈 자체가 없어 정의되지 않는 것이다
 * @param trialCostKrw  체험 업체가 태운 모델 원가. 매출이 0원이라 <b>전액이 손실</b>인데
 *                      수익성 화면의 원가율은 이들을 0%/정상으로 표시한다(정가가 0이라
 *                      나눌 수 없다). 여기서만 드러난다
 */
public record RevenueSummaryResponse(
        String period,
        long mrrKrw,
        long billedKrw,
        long collectedKrw,
        long refundedKrw,
        long outstandingKrw,
        long paidCount,
        long unpaidCount,
        BigDecimal modelCostKrw,
        BigDecimal marginKrw,
        Integer marginPercent,
        BigDecimal trialCostKrw,
        long trialTenantCount) {
}
