"use client";

import * as Dialog from "@radix-ui/react-dialog";
import type { ReactNode } from "react";

import { Button } from "@/shared/common/button";

/**
 * 확인 모달.
 *
 * radix 를 쓰는 이유는 프로토타입의 `div + onclick` 스크림이 키보드로 닫히지 않고
 * 포커스가 뒤 화면으로 새기 때문이다 (docs/IMPROVEMENTS.md 접근성 부채).
 * Esc 로 닫기·포커스 트랩·`aria-modal` 을 전부 여기서 얻는다.
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
  onConfirm: () => void;
  confirmDisabled?: boolean;
  pending?: boolean;
  error?: string | null;
}) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-ink/45" />
        <Dialog.Content className="fixed top-1/2 left-1/2 z-50 w-[calc(100%-2rem)] max-w-[460px] -translate-x-1/2 -translate-y-1/2 rounded-xl border border-line bg-card shadow-2xl">
          <header className="border-b border-line-2 px-5 py-4">
            <Dialog.Title className="text-[15px] font-semibold">{title}</Dialog.Title>
            {description ? (
              <Dialog.Description className="mt-1 text-[12.5px] text-slate-2">
                {description}
              </Dialog.Description>
            ) : null}
          </header>

          <div className="px-5 py-5">
            {warning ? (
              <div className="mb-4 rounded-lg bg-brick-soft px-3 py-2.5 text-[12.5px] leading-relaxed text-[#8a2e1e]">
                {warning}
              </div>
            ) : null}
            {children}
            {error ? <p className="mt-2 text-[12.5px] text-brick">{error}</p> : null}
          </div>

          <footer className="flex justify-end gap-2 border-t border-line-2 px-5 py-3.5">
            <Dialog.Close asChild>
              <Button>취소</Button>
            </Dialog.Close>
            <Button
              variant={confirmVariant}
              onClick={onConfirm}
              disabled={confirmDisabled || pending}
            >
              {pending ? "처리 중" : confirmLabel}
            </Button>
          </footer>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
