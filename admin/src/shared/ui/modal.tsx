"use client";

import * as Dialog from "@radix-ui/react-dialog";
import type { ReactNode } from "react";

import { Button } from "@/shared/common/button";

/**
 * 모달.
 *
 * radix 를 쓰는 이유는 프로토타입의 `div + onclick` 스크림이 키보드로 닫히지 않고
 * 포커스가 뒤 화면으로 새기 때문이다 (docs/IMPROVEMENTS.md 접근성 부채).
 * Esc 로 닫기·포커스 트랩·`aria-modal` 을 전부 여기서 얻는다.
 *
 * <p><b>높이는 화면을 넘지 않는다.</b> 전에는 상한도 스크롤도 없이 화면 중앙에 고정돼 있어,
 * 내용이 길면 위아래로 잘렸다 — 그리고 잘린 <b>윗부분에는 닿을 수도 없었다.</b> 화면 전체가
 * 스크롤되지 않고 요소만 화면 밖으로 나가기 때문이다. 머리말과 버튼은 제자리에 두고
 * 본문만 스크롤한다.
 *
 * <p>{@code dvh} 를 쓰는 이유 — 모바일 브라우저의 주소창이 접히고 펴질 때 {@code vh} 는
 * 그것을 따라가지 않아 버튼이 화면 밖에 남는다.
 *
 * <p>{@code onConfirm} 이 없으면 <b>읽는 모달</b>이다(사용 가이드 등). 그때는 확인 버튼을
 * 그리지 않고 닫기만 둔다 — 아무 일도 하지 않는 "확인"은 무엇이 확정되는지 헷갈리게 한다.
 */
export function Modal({
  open,
  onOpenChange,
  title,
  description,
  warning,
  children,
  confirmLabel = "확인",
  confirmVariant = "primary",
  onConfirm,
  confirmDisabled,
  pending,
  error,
  size = "sm",
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: ReactNode;
  /** 되돌리기 어려운 조작 앞에 붙이는 경고 상자. */
  warning?: ReactNode;
  children?: ReactNode;
  confirmLabel?: string;
  confirmVariant?: "primary" | "accent" | "danger";
  /** 없으면 읽는 모달이다 — 닫기 버튼만 나온다. */
  onConfirm?: () => void;
  confirmDisabled?: boolean;
  pending?: boolean;
  error?: string | null;
  /** 긴 글을 담는 모달은 `lg`. 좁은 폭에 긴 글을 넣으면 줄이 잘게 끊겨 읽기 어렵다. */
  size?: "sm" | "lg";
}) {
  const readOnly = onConfirm === undefined;

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-ink/45" />
        <Dialog.Content
          className={[
            "fixed top-1/2 left-1/2 z-50 -translate-x-1/2 -translate-y-1/2",
            "flex max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] flex-col",
            "rounded-xl border border-line bg-card shadow-2xl",
            size === "lg" ? "max-w-[640px]" : "max-w-[460px]",
          ].join(" ")}
        >
          <header className="shrink-0 border-b border-line-2 px-5 py-4">
            <Dialog.Title className="text-[15px] font-semibold">{title}</Dialog.Title>
            {description ? (
              <Dialog.Description className="mt-1 text-[12.5px] text-slate-2">
                {description}
              </Dialog.Description>
            ) : null}
          </header>

          {/* 여기만 스크롤한다. 머리말과 버튼은 늘 보이는 자리에 남는다. */}
          <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5">
            {warning ? (
              <div className="mb-4 rounded-lg bg-brick-soft px-3 py-2.5 text-[12.5px] leading-relaxed text-[#8a2e1e]">
                {warning}
              </div>
            ) : null}
            {children}
            {error ? <p className="mt-2 text-[12.5px] text-brick">{error}</p> : null}
          </div>

          <footer className="flex shrink-0 justify-end gap-2 border-t border-line-2 px-5 py-3.5">
            <Dialog.Close asChild>
              <Button>{readOnly ? "닫기" : "취소"}</Button>
            </Dialog.Close>
            {readOnly ? null : (
              <Button
                variant={confirmVariant}
                onClick={onConfirm}
                disabled={confirmDisabled || pending}
              >
                {pending ? "처리 중" : confirmLabel}
              </Button>
            )}
          </footer>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
