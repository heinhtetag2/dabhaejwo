"use client";

import { create } from "zustand";

/**
 * 토큰만 들고 있는다. 담당자·업체 정보는 서버 상태이므로 TanStack Query 가 갖는다
 * (`entities/auth/session`). 같은 데이터를 두 곳에 두면 반드시 어긋난다.
 *
 * shared 레이어라 도메인 타입을 알지 못한다 — 여기 있는 것은 문자열 토큰뿐이다.
 */
interface AuthState {
  /** access token 은 메모리에만. localStorage 금지 (kickoff-prompt.md §1.3). */
  accessToken: string | null;
  /**
   * 리프레시 토큰도 메모리에 둔다. 그래서 새로고침하면 다시 로그인해야 한다.
   * 지속 세션은 서버가 httpOnly 쿠키로 내려줘야 안전하게 된다 — docs/IMPROVEMENTS.md 참조.
   */
  refreshToken: string | null;
  signIn: (accessToken: string, refreshToken: string) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  signIn: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
  signOut: () => set({ accessToken: null, refreshToken: null }),
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
