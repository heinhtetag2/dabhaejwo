"use client";

import { create } from "zustand";

export type TenantMemberRole = "OWNER" | "EDITOR" | "VIEWER";

export interface TenantMember {
  memberId: string;
  tenantId: string;
  tenantName: string;
  role: TenantMemberRole;
}

/**
 * 운영팀이 대리 접속 중일 때의 정보.
 *
 * null 이 아니면 전 화면 상단에 배너를 고정 노출한다 — 운영자가 자기 계정으로
 * 착각한 채 데이터를 변경하는 사고를 막기 위함이다 (tenant-plan.md §6.2).
 */
export interface ImpersonationInfo {
  sessionId: string;
  operatorName: string;
  reason: string;
  expiresAt: string;
}

interface AuthState {
  /** access token 은 메모리에만. localStorage 금지 (kickoff-prompt.md §1.3). */
  accessToken: string | null;
  member: TenantMember | null;
  impersonation: ImpersonationInfo | null;
  signIn: (accessToken: string, member: TenantMember) => void;
  setImpersonation: (info: ImpersonationInfo | null) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  member: null,
  impersonation: null,
  signIn: (accessToken, member) => set({ accessToken, member }),
  setImpersonation: (impersonation) => set({ impersonation }),
  signOut: () => set({ accessToken: null, member: null, impersonation: null }),
}));

export function currentAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}

/**
 * 편집 가능한 역할인가. 대리 접속 중이면 서버가 VIEWER 로 낮춰 발급하므로
 * 여기서도 자연히 false 가 된다. 다만 권한의 진실은 서버다 — 이건 UX 일 뿐이다.
 */
export function canEdit(role: TenantMemberRole | undefined): boolean {
  return role === "OWNER" || role === "EDITOR";
}
