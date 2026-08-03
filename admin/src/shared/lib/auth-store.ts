"use client";

import { create } from "zustand";

export type OperatorRole = "OPS_ADMIN" | "CS" | "SALES" | "DEV";

export interface Operator {
  id: string;
  name: string;
  role: OperatorRole;
}

interface AuthState {
  /**
   * access token 은 메모리에만 둔다. localStorage 금지 (kickoff-prompt.md §1.3).
   * 새로고침하면 사라지고 refresh 로 다시 받는다.
   */
  accessToken: string | null;
  operator: Operator | null;
  signIn: (accessToken: string, operator: Operator) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  operator: null,
  signIn: (accessToken, operator) => set({ accessToken, operator }),
  signOut: () => set({ accessToken: null, operator: null }),
}));

/** 화면 밖(http-client)에서 토큰을 읽기 위한 접근점. */
export function currentAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}
