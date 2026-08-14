"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import type { OtpChallenge } from "@/entities/auth/session";
import type { Language } from "@/shared/lib/language";

import { LOGIN_TEXT } from "./login-content";
import { LoginOtpForm, LoginPasswordForm } from "./login-form";

/**
 * 로그인 페이지 — 필드·검증·제출·단계 전환은 `LoginPasswordForm`/`LoginOtpForm` 이 갖는다
 * (모달과 공유, `login-form.tsx`). 이 컴포넌트는 페이지 전용 틀(제목·여백)과 단계별
 * 제목 전환만 맡는다 — `signup-view.tsx` 와 같은 구조.
 *
 * <p><b>두 단계다.</b> 비밀번호가 맞으면 인증 코드가 메일로 가고, 그 코드를 맞혀야 들어온다.
 * 비밀번호 하나가 새면 계정이 통째로 넘어가는 구조를 없앤다.
 *
 * <p>단계를 라우트로 나누지 않는다 — 새로고침하면 챌린지가 사라지는데, URL 이 남아 있으면
 * 사용자는 되돌아갈 수 있다고 오해한다. 한 화면 안에서 바꾼다.
 *
 * <p>`language` 는 페이지에서 `getLanguage()` 로 읽어 props 로 내려받는다 — 이 컴포넌트는
 * `"use client"` 라 쿠키를 직접 못 읽는다(header·footer·pricing 과 같은 이유).
 */
export function LoginView({ language }: { language: Language }) {
  const t = LOGIN_TEXT[language];
  const router = useRouter();
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      {challenge ? (
        <>
          <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">{t.otpTitle}</h1>
          <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">
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
          <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">{t.title}</h1>
          <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">{t.subtitle}</p>
          <LoginPasswordForm language={language} onIssued={setChallenge} />
        </>
      )}
    </div>
  );
}
