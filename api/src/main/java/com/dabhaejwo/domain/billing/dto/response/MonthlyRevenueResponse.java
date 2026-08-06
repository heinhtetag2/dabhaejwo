package com.dabhaejwo.domain.billing.dto.response;

import java.math.BigDecimal;

/**
 * 월별 정산 한 줄 (api-contracts.md §6-1). 최신 달이 배열 앞에 온다.
 *
 * @param convertedCount    그 달에 가입한 업체 중 <b>지금까지 한 번이라도</b> 결제한 곳.
 *                          "그 달에 첫 결제가 발생한 수"로 세면 분자와 분모가 서로 다른
 *                          모집단이라 비율이 아무 뜻도 갖지 못한다
 * @param conversionPercent 가입이 없던 달은 null
 * @param cohortOpen        그 달 말일 가입자의 체험이 아직 안 끝났다. 전환율이 더 오를 수
 *                          있는 달이라 화면이 그 사실을 밝힌다 — 안 밝히면 운영자가
 *                          "이번 달 전환율이 급락했다"고 잘못 읽는다
 */
public record MonthlyRevenueResponse(
        String period,
        long billedKrw,
        long collectedKrw,
        long refundedKrw,
        long failedCount,
        BigDecimal modelCostKrw,
        BigDecimal marginKrw,
        long signupCount,
        long convertedCount,
        Integer conversionPercent,
        long churnedCount,
        boolean cohortOpen) {
}
