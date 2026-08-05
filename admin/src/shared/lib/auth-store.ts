"use client";

import { create } from "zustand";

export type OperatorRole = "OPS_ADMIN" | "CS" | "SALES" | "DEV";

export interface Operator {
  id: string;
  name: string;
  email?: string;
  role: OperatorRole;
}

interface AuthState {
  /**
   * access token 은 메모리에만 둔다. localStorage 금지 (kickoff-prompt.md §1.3).
   * 새로고침하면 사라지고 다시 로그인해야 한다 — 지속 세션은 서버가 리프레시 토큰을
   * httpOnly 쿠키로 내려줘야 안전해진다 (docs/IMPROVEMENTS.md).
   */
  accessToken: string | null;
  refreshToken: string | null;
  operator: Operator | null;
  signIn: (tokens: { accessToken: string; refreshToken: string }, operator: Operator) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  operator: null,
  signIn: ({ accessToken, refreshToken }, operator) =>
    set({ accessToken, refreshToken, operator }),
  signOut: () => set({ accessToken: null, refreshToken: null, operator: null }),
}));

/** 화면 밖(http-client)에서 토큰을 읽기 위한 접근점. */
export function currentAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}

/**
 * 역할별 권한. **서버가 진실이다** — 여기서 버튼을 숨기는 것은 UX 이지 보안이 아니고,
 * 각 API 가 `@RequirePermission` 으로 다시 검증한다.
 *
 * 매핑은 api 의 `OperatorRole` enum 과 같은 커밋에서 움직인다.
 * 출처: docs/plan/admin-console-plan.md §7 권한 매트릭스.
 */
export const ROLE_PERMISSIONS: Record<OperatorRole, readonly string[]> = {
  OPS_ADMIN: ["*"],
  CS: [
    "TENANT_READ",
    "TENANT_NOTE_WRITE",
    "TENANT_IMPERSONATE",
    "QUOTA_GRANT",
    "JOB_READ",
    "JOB_RETRY",
    "TICKET_READ",
    "TICKET_WRITE",
  ],
  SALES: [
    "TENANT_READ",
    "TENANT_NOTE_WRITE",
    "TENANT_TRIAL_WRITE",
    "TENANT_PLAN_WRITE",
    "PROFITABILITY_READ",
    "PLAN_READ",
    "PLAN_WRITE",
    "TICKET_READ",
  ],
  DEV: [
    "TENANT_READ",
    "TENANT_IMPERSONATE",
    "AI_USAGE_READ",
    "JOB_READ",
    "JOB_RETRY",
    "FLAG_READ",
    "FLAG_WRITE",
    "TICKET_READ",
  ],
};

export function can(operator: Operator | null, permission: string): boolean {
  if (!operator) return false;
  const granted = ROLE_PERMISSIONS[operator.role];
  return granted.includes("*") || granted.includes(permission);
}
