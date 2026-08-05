"use client";

import { create } from "zustand";

/**
 * 토큰만 들고 있는다. 담당자·업체 정보는 서버 상태이므로 TanStack Query 가 갖는다
 * (`entities/auth/session`). 같은 데이터를 두 곳에 두면 반드시 어긋난다.
 *
 * shared 레이어라 도메인 타입을 알지 못한다 — 여기 있는 것은 문자열 토큰뿐이다.
 */
/**
 * 세션이 있는지 <b>아직 모르는</b> 상태를 구분한다.
 *
 * 새로고침 직후에는 메모리가 비어 있지만 리프레시 쿠키는 살아 있을 수 있다. 이때
 * "토큰이 없다 = 로그아웃"으로 단정하면 **복원해 보기도 전에 로그인 화면으로 튕긴다** —
 * 실제로 그렇게 동작했었다.
 */
export type SessionStatus = "unknown" | "authenticated" | "anonymous";

interface AuthState {
  /** access token 은 메모리에만. localStorage 금지 (kickoff-prompt.md §1.3). */
  accessToken: string | null;
  /** 쿠키가 진실이라 이제 쓰이지 않는다. 로그인 응답 형태를 그대로 받기 위해 남겨 둔다. */
  refreshToken: string | null;
  status: SessionStatus;
  signIn: (accessToken: string, refreshToken?: string | null) => void;
  /** 복원 성공 — 액세스 토큰만 갈아끼운다(만료 재발급에도 쓴다). */
  setAccessToken: (accessToken: string) => void;
  /** 복원할 세션이 없었다. 이제 로그인으로 보내도 된다. */
  markAnonymous: () => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  status: "unknown",
  signIn: (accessToken, refreshToken = null) =>
    set({ accessToken, refreshToken, status: "authenticated" }),
  setAccessToken: (accessToken) => set({ accessToken, status: "authenticated" }),
  markAnonymous: () => set({ accessToken: null, refreshToken: null, status: "anonymous" }),
  signOut: () => set({ accessToken: null, refreshToken: null, status: "anonymous" }),
}));

export function currentAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}

/**
 * 편집 가능한 역할인가. 대리 접속 중이면 서버가 VIEWER 로 낮춰 발급하므로
 * 여기서도 자연히 false 가 된다. 다만 권한의 진실은 서버다 — 이건 UX 일 뿐이다.
 */
export function canEdit(role: "OWNER" | "EDITOR" | "VIEWER" | undefined): boolean {
  return role === "OWNER" || role === "EDITOR";
}
