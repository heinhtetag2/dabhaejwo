"use client";

import { create } from "zustand";

export type OperatorRole = "OPS_ADMIN" | "CS" | "SALES" | "DEV";

export interface Operator {
  id: string;
  name: string;
  email?: string;
  role: OperatorRole;
}

/**
 * 세션이 있는지 <b>아직 모르는</b> 상태를 구분한다.
 *
 * 새로고침 직후에는 메모리가 비어 있지만 리프레시 쿠키는 살아 있을 수 있다. 이때
 * "토큰이 없다 = 로그아웃"으로 단정하면 **복원해 보기도 전에 로그인 화면으로 튕긴다** —
 * 실제로 그렇게 동작했었다. `unknown` 인 동안에는 아무 판단도 하지 않는다.
 */
export type SessionStatus = "unknown" | "authenticated" | "anonymous";

interface AuthState {
  /**
   * access token 은 메모리에만 둔다. localStorage 금지 (kickoff-prompt.md §1.3).
   * 새로고침하면 사라지지만, 리프레시 토큰이 httpOnly 쿠키로 남아 있어
   * 세션은 되살아난다 (`widgets/auth-guard`).
   */
  accessToken: string | null;
  /** 쿠키가 진실이라 이제 쓰이지 않는다. 로그인 응답 형태를 그대로 받기 위해 남겨 둔다. */
  refreshToken: string | null;
  operator: Operator | null;
  status: SessionStatus;
  signIn: (tokens: { accessToken: string; refreshToken?: string | null }, operator: Operator) => void;
  /** 복원 중간 단계 — 토큰은 얻었지만 아직 내가 누구인지 모른다. */
  setAccessToken: (accessToken: string) => void;
  /** 복원할 세션이 없었다. 이제 로그인으로 보내도 된다. */
  markAnonymous: () => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  operator: null,
  status: "unknown",
  signIn: ({ accessToken, refreshToken = null }, operator) =>
    set({ accessToken, refreshToken, operator, status: "authenticated" }),
  setAccessToken: (accessToken) => set({ accessToken }),
  markAnonymous: () =>
    set({ accessToken: null, refreshToken: null, operator: null, status: "anonymous" }),
  signOut: () =>
    set({ accessToken: null, refreshToken: null, operator: null, status: "anonymous" }),
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
