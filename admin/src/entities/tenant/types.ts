/**
 * 업체 리소스 타입.
 *
 * 키는 docs/architecture/api-contracts.md §2 와 완전히 일치해야 한다.
 * 변환 레이어를 두지 않는다 — 계약 ↔ 코드가 어긋나면 코드가 틀린 것이다.
 */

export type TenantStatus = "TRIAL" | "ACTIVE" | "SUSPENDED" | "CHURNED";

export type TenantSort = "COST_RATIO_DESC" | "NAME_ASC" | "CONV_DESC";

export type TenantFilter =
  | "ALL"
  | "TRIAL"
  | "PAYMENT_FAILED"
  | "COST_EXCEEDED"
  | "INACTIVE_7D"
  | "SUSPENDED"
  | "CHURNED";

export interface PlanRef {
  id: string;
  name: string;
}

/** 목록 항목. Detail 의 부분집합이며 겹치는 필드는 이름·타입이 동일하다. */
export interface TenantSummary {
  id: string;
  name: string;
  primaryDomain: string;
  status: TenantStatus;
  plan: PlanRef | null;
  convCount: number;
  convLimit: number;
  costKrw: number;
  billedKrw: number;
  costRatioPercent: number;
}

export interface TenantDetail extends Omit<TenantSummary, "plan"> {
  publishableKey: string;
  currency: string;
  plan: (PlanRef & { monthlyFee: number }) | null;
  docCount: number;
  docLimit: number;
  /** 0이면 화면에서 강조한다 — 원가 급증의 선행 지표다. */
  faqCount: number;
  savedAnswerPercent: number;
  joinedDate: string;
  trialEndsAt: string | null;
  nextBillingDate: string | null;
  lastSeenAt: string | null;
}

export interface TenantListParams {
  q?: string;
  filter?: TenantFilter;
  sort?: TenantSort;
  page?: number;
  size?: number;
}
