"use client";

import { useRouter } from "next/navigation";

import { Modal } from "@/shared/common/modal";
import type { Language } from "@/shared/lib/language";
import { useLoginModalStore } from "@/shared/lib/login-modal-store";
import { useSignupModalStore } from "@/shared/lib/signup-modal-store";

import { SIGNUP_TEXT } from "./signup-content";
import { SignupForm } from "./signup-form";

/**
 * 가입 모달 — 헤더·푸터·랜딩·요금제의 "무료로 시작" 류 CTA(`shared/ui/signup-cta-link.tsx`)가
 * 연다. 공개 레이아웃(`app/(public)/layout.tsx`)에 한 번만 마운트한다 — 페이지마다 각자
 * 두면 열림 상태가 페이지 전환에 끊긴다.
 *
 * <p>성공하면 모달을 닫을 필요가 없다 — 대시보드로 즉시 이동하며 이 트리는 그대로 언마운트된다.
 */
export function SignupModal({ language }: { language: Language }) {
  const t = SIGNUP_TEXT[language];
  const router = useRouter();
  const isOpen = useSignupModalStore((state) => state.isOpen);
  const close = useSignupModalStore((state) => state.close);
  const openLogin = useLoginModalStore((state) => state.open);

  function switchToLogin() {
    close();
    openLogin();
  }

  return (
    <Modal open={isOpen} onOpenChange={(open) => (open ? undefined : close())} title={t.title}>
      {/*
        `/app` 으로 보낸다. 서비스가 경로에 들어간 뒤로(M3) 대시보드의 첫 화면은
        서비스 id 를 알아야 정해지는데, 가입 직후에는 아직 그 값이 없다 —
        `/app` 이 컨텍스트를 읽어 기본 서비스로 넘긴다.
      */}
      <SignupForm
        language={language}
        onSuccess={() => router.replace("/app")}
        onSwitchToLogin={switchToLogin}
      />
    </Modal>
  );
}
