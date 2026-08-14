"use client";

import type { MouseEvent } from "react";

import { useLoginModalStore } from "./login-modal-store";

/**
 * "로그인" 류 CTA 의 클릭만 가로챈다 — `use-signup-cta-click.ts` 와 같은 이유·같은 방식.
 * 수정 없는 좌클릭만 가로채 모달을 열고, Ctrl/Cmd/Shift/Alt+클릭·중클릭은 그대로
 * `/login` 페이지로 보낸다.
 */
export function useLoginCtaClick() {
  const open = useLoginModalStore((state) => state.open);

  return function handleLoginCtaClick(event: MouseEvent<HTMLAnchorElement>) {
    const isModifiedClick =
      event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey;
    if (isModifiedClick) return;
    event.preventDefault();
    open();
  };
}
