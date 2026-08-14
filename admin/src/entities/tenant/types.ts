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

export interface OperatorRef {
  id: string;
  name: string;
}

/** 목록 항목. Detail 의 부분집합이며 겹치는 필드는 이름·타입이 동일하다. */
export interface TenantSummary {
  id: string;
  name: string;
  primaryDomain: string;
  /** 서비스 수. 1이면 화면이 지금과 글자 하나 다르지 않다. */
  botCount: number;
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

/** 필터 칩별 건수. 0인 칩도 내려온다 — 흐리게 처리하되 숨기지 않는다. */
export interface TenantFilterCounts {
  all: number;
  trial: number;
  paymentFailed: number;
  costExceeded: number;
  inactive7d: number;
  suspended: number;
  churned: number;
}

export type TenantActivityType =
  | "CHANGE_PLAN"
  | "GRANT_QUOTA"
  | "SUSPEND"
  | "CHURN"
  | "EXTEND_TRIAL"
  | "IMPERSONATE"
  | "VIEW_CONVERSATIONS"
  | "MODEL_PRICE_WRITE"
  | "COST_GUARD_WRITE"
  | "PAYMENT"
  | "NOTE";

export interface TenantActivity {
  id: string;
  type: TenantActivityType;
  at: string;
  summary: string;
  reason: string | null;
  /** 시스템이 만든 항목(결제)은 행위자가 없다. */
  operator: OperatorRef | null;
}

/** 내부 메모. 누적이며 수정·삭제 경로가 없다. */
export interface TenantNote {
  id: number;
  body: string;
  operator: OperatorRef | null;
  createdAt: string;
}

export interface QuotaOverride {
  id: number;
  periodMonth: string;
  convDelta: number;
  docDelta: number;
  reason: string;
  operator: OperatorRef | null;
  createdAt: string;
}

export type ImpersonationStatus = "ACTIVE" | "ENDED" | "EXPIRED" | "REVOKED";

export interface ImpersonationSession {
  sessionId: string;
  tenant: { id: string; name: string };
  reason: string;
  /** 종료 응답에는 토큰이 없다 — 그대로 다시 쓰면 끝난 세션으로 접속하게 된다. */
  accessToken: string | null;
  startedAt: string;
  expiresAt: string;
  status: ImpersonationStatus;
}

export type BotStatus = "ACTIVE" | "PAUSED" | "DELETING";

/** 운영 콘솔이 보는 서비스. 업체 대시보드와 겹치는 필드는 이름·타입이 같다. */
export interface OpsBot {
  id: string;
  name: string;
  primaryDomain: string;
  publishableKey: string;
  status: BotStatus;
  defaultBot: boolean;
  /** null 이면 위젯이 아직 한 번도 호출되지 않았다 — 설치가 안 됐다는 뜻이다. */
  lastCalledAt: string | null;
  createdAt: string;
  allowedOrigins: Array<{ id: string; origin: string; lastCalledAt: string | null }>;
}
