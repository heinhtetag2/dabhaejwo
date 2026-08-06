package com.dabhaejwo.domain.billing.dto.response;

import java.time.LocalDate;

/**
 * 요금제 변경 결과.
 *
 * @param charged      이번에 <b>실제로 돈이 나갔는가</b>. 화면이 "결제되었습니다"와
 *                     "다음 청구일부터 적용됩니다"를 갈라 말해야 한다
 * @param amountKrw    결제했다면 그 금액. 아니면 0
 * @param receiptUrl   영수증. 결제하지 않았으면 null
 * @param nextBillingDate 다음 청구 예정일
 */
public record PlanChangeResponse(String planName, boolean charged, int amountKrw,
                                 String receiptUrl, LocalDate nextBillingDate) {
}
