"use client";

import { create } from "zustand";

interface LoginModalState {
  isOpen: boolean;
  open: () => void;
  close: () => void;
}

/**
 * 로그인 모달의 열림 상태만 담는다 — 헤더·푸터 등 여러 슬라이스가 같은 모달을 함께
 * 트리거한다. `signup-modal-store.ts` 와 같은 이유로 Zustand 다.
 */
export const useLoginModalStore = create<LoginModalState>((set) => ({
  isOpen: false,
  open: () => set({ isOpen: true }),
  close: () => set({ isOpen: false }),
}));
