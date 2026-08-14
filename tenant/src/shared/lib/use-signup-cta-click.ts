"use client";

import type { MouseEvent } from "react";

import { useSignupModalStore } from "./signup-modal-store";

/**
 * "무료로 시작" 류 CTA 의 클릭만 가로챈다. `href` 는 그대로 `/signup` 을 가리키는 실제
 * 앵커에 이 핸들러만 얹어 쓴다 — `LinkButton` 자체의 이유와 같다: onClick + router.push 로만
 * 이동을 대신하면 새 탭 열기·주소 복사가 막히고 스크린 리더에도 버튼으로 읽힌다
 * (`shared/common/button.tsx`). 그래서 수정 없는 좌클릭만 가로채 모달을 열고,
 * Ctrl/Cmd/Shift/Alt+클릭·중클릭은 그대로 `/signup` 페이지로 보낸다 — 앵커 기본 동작을
 * 죽이지 않는 점진적 향상(progressive enhancement)이다.
 *
 * <p>스타일은 건드리지 않는다 — 헤더·푸터·랜딩·요금제가 이미 각자 다른 컴포넌트
 * (`LinkButton`·`FooterLink`·평문 `Link`)로 CTA 를 그리고 있어, 감싸는 컴포넌트를 새로 두면
 * 스타일 시스템이 하나 더 생기거나 기존 모양이 깨진다. 기존 앵커에 이 핸들러만 얹는다.
 */
export function useSignupCtaClick() {
  const open = useSignupModalStore((state) => state.open);

  return function handleSignupCtaClick(event: MouseEvent<HTMLAnchorElement>) {
    const isModifiedClick =
      event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey;
    if (isModifiedClick) return;
    event.preventDefault();
    open();
  };
}
