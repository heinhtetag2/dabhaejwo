"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import type { ReactNode } from "react";

/**
 * 모달 — 내용을 스스로 갖춘(자체 제출 버튼이 있는 폼 등) 자리에 쓴다.
 *
 * <p>admin `shared/ui/modal.tsx` 와 같은 이유로 radix 를 쓴다 — `div + onclick` 스크림은
 * 키보드로 닫히지 않고 포커스가 뒤 화면으로 샌다. Esc·포커스 트랩·`aria-modal` 을
 * 여기서 얻는다. admin 것과 API 가 다른 이유: admin 은 확인/취소 액션(승인·거부)을 감싸는
 * 용도라 푸터에 확인 버튼이 내장돼 있다. 이건 첫 용도(회원가입 폼)부터 내용이 자기
 * 제출 버튼을 이미 갖고 있어 푸터를 얹으면 버튼이 두 개가 된다 — 그래서 헤더(제목+닫기)와
 * 스크롤 본문만 있다.
 *
 * <p>{@code dvh} 를 쓰는 이유 — 모바일 브라우저 주소창이 접히고 펴질 때 {@code vh} 는
 * 따라가지 않아 버튼이 화면 밖에 남는다.
 */
export function Modal({
  open,
  onOpenChange,
  title,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  children: ReactNode;
}) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-ink/45" />
        <Dialog.Content className="fixed top-1/2 left-1/2 z-50 flex max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-[440px] -translate-x-1/2 -translate-y-1/2 flex-col rounded-xl border border-line bg-card shadow-2xl">
          <div className="flex shrink-0 items-center justify-between border-b border-line-2 px-5 py-4">
            <Dialog.Title className="text-[15px] font-semibold">{title}</Dialog.Title>
            <Dialog.Close className="rounded-full p-1.5 text-slate-2 transition-colors hover:bg-line-2 hover:text-ink">
              <X aria-hidden className="size-4" />
              <span className="sr-only">닫기</span>
            </Dialog.Close>
          </div>

          {/* 여기만 스크롤한다 — 머리말은 늘 보이는 자리에 남는다(admin Modal과 같은 이유). */}
          <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5">{children}</div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
