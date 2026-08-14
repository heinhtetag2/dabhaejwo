"use client";

import { create } from "zustand";

interface SignupModalState {
  isOpen: boolean;
  open: () => void;
  close: () => void;
}

/**
 * 회원가입 모달의 열림 상태만 담는다 — 헤더·푸터·랜딩·요금제 등 여러 슬라이스가 같은
 * 모달을 함께 트리거한다. 서버 데이터가 아니라 UI 상태라 TanStack Query 가 아닌
 * Zustand 다(frontend-rules: 전역 UI 상태만 Zustand). shared 레이어라 도메인 타입은 모른다.
 */
export const useSignupModalStore = create<SignupModalState>((set) => ({
  isOpen: false,
  open: () => set({ isOpen: true }),
  close: () => set({ isOpen: false }),
}));
