/** 정산. 키는 api-contracts.md §6-1 과 일치한다. */

export type BillingStatus = "PAID" | "FAILED" | "PENDING" | "REFUNDED";

/**
 * 이번 달 정산 요약.
 *
 * 매출을 한 단어로 부르지 않는다 — 아래 넷은 전부 다른 값이고 서로 대체할 수 없다.
 * `mrrKrw`(앞으로 들어올 돈) · `billedKrw`(청구) · `collectedKrw`(**실제 받은 돈**) ·
 * `outstandingKrw`(쫓아가야 할 돈).
 */
export interface RevenueSummary {
  period: string;
  mrrKrw: number;
  billedKrw: number;
  collectedKrw: number;
  refundedKrw: number;
  outstandingKrw: number;
  paidCount: number;
  unpaidCount: number;
  modelCostKrw: number;
  marginKrw: number;
  /** 수납액이 0이면 null — 0% 는 "남는 게 없다"로 읽힌다. */
  marginPercent: number | null;
  /** 체험 업체 모델 원가. 매출이 0원이라 전액이 손실이다. */
  trialCostKrw: number;
  trialTenantCount: number;
}

export interface MonthlyRevenue {
  period: string;
  billedKrw: number;
  collectedKrw: number;
  refundedKrw: number;
  failedCount: number;
  modelCostKrw: number;
  marginKrw: number;
  signupCount: number;
  convertedCount: number;
  /** 가입이 없던 달은 null. */
  conversionPercent: number | null;
  churnedCount: number;
  /** 말일 가입자의 체험이 아직 안 끝났다 — 전환율이 더 오를 수 있는 달이다. */
  cohortOpen: boolean;
}

export interface BillingRecordItem {
  id: number;
  tenant: { id: string; name: string };
  planName: string | null;
  period: string;
  amountKrw: number;
  status: BillingStatus;
  attempts: number;
  failureReason: string | null;
  orderId: string | null;
  /** 분쟁 시 토스 기록과 대조할 값. 결제를 실행할 수 있는 값이 아니다. */
  paymentKey: string | null;
  paidAt: string | null;
  method: string | null;
  receiptUrl: string | null;
}
