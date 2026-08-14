"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import type { OtpChallenge } from "@/entities/auth/session";
import { Modal } from "@/shared/common/modal";
import type { Language } from "@/shared/lib/language";
import { useLoginModalStore } from "@/shared/lib/login-modal-store";

import { LOGIN_TEXT } from "./login-content";
import { LoginOtpForm, LoginPasswordForm } from "./login-form";

/**
 * 로그인 모달 — 헤더·푸터 등의 "로그인" CTA(`shared/ui/login-cta-link.tsx`)가 연다.
 * 공개 레이아웃(`app/(public)/layout.tsx`)에 한 번만 마운트한다 — `signup-modal.tsx` 와
 * 같은 이유.
 *
 * <p>제목이 단계마다 바뀐다(비밀번호 단계 vs 인증 코드 단계) — 이 컴포넌트가 `challenge`
 * 상태를 직접 들고 있어야 `Modal` 의 제목 표시줄에 지금 단계에 맞는 문구를 넘길 수 있다.
 *
 * <p>성공하면 모달을 닫을 필요가 없다 — 대시보드로 즉시 이동하며 이 트리는 그대로 언마운트된다.
 * 닫혔다 다시 열리면 항상 비밀번호 단계부터 시작한다 — 인증 코드는 몇 분 안에 만료되므로
 * 이전 챌린지를 남겨두면 오히려 혼란스럽다.
 */
export function LoginModal({ language }: { language: Language }) {
  const t = LOGIN_TEXT[language];
  const router = useRouter();
  const isOpen = useLoginModalStore((state) => state.isOpen);
  const close = useLoginModalStore((state) => state.close);
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);

  function handleOpenChange(open: boolean) {
    if (open) return;
    close();
    setChallenge(null);
  }

  return (
    <Modal open={isOpen} onOpenChange={handleOpenChange} title={challenge ? t.otpTitle : t.title}>
      {challenge ? (
        <>
          <p className="text-[15.5px] leading-[1.7] text-slate">
            {t.otpSubtitleBefore}
            <b className="font-semibold text-ink">{challenge.maskedEmail}</b>
            {t.otpSubtitleAfter(challenge.ttlMinutes)}
          </p>
          <LoginOtpForm
            language={language}
            challenge={challenge}
            onBack={() => setChallenge(null)}
            onSuccess={() => router.replace("/app")}
          />
        </>
      ) : (
        <>
          <p className="text-[15.5px] leading-[1.7] text-slate">{t.subtitle}</p>
          <LoginPasswordForm language={language} onIssued={setChallenge} />
        </>
      )}
    </Modal>
  );
}
