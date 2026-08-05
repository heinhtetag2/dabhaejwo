/** 감사 기록. 키는 api-contracts.md §8 과 일치한다. */

import type { OperatorRef } from "@/entities/tenant";

export type AuditAction =
  | "IMPERSONATE"
  | "VIEW_CONVERSATIONS"
  | "CHANGE_PLAN"
  | "GRANT_QUOTA"
  | "SUSPEND"
  | "CHURN"
  | "EXTEND_TRIAL"
  | "MODEL_PRICE_WRITE"
  | "COST_GUARD_WRITE"
  | "ACTIVATE"
  | "PLAN_WRITE"
  | "FLAG_WRITE"
  | "TICKET_WRITE";

/**
 * 감사 기록 한 줄.
 *
 * 쓰기 타입이 없는 것이 설계다 — 적재는 서버 내부에서만 일어나고,
 * 수정·삭제는 DB 트리거가 막는다.
 */
export interface AuditLog {
  id: number;
  at: string;
  operator: OperatorRef;
  action: AuditAction;
  /** 업체와 무관한 행위(단가 수정·기능 공개)는 null. */
  tenant: { id: string; name: string } | null;
  reason: string;
  meta: Record<string, unknown>;
}

export interface AuditListParams {
  tenantId?: string;
  operatorId?: string;
  action?: AuditAction;
  from?: string;
  to?: string;
  page?: number;
}
