"use client";

import { useRouter } from "next/navigation";

import type { Language } from "@/shared/lib/language";

import { SIGNUP_TEXT } from "./signup-content";
import { SignupForm } from "./signup-form";

/**
 * 가입 페이지 — 필드·검증·제출은 `SignupForm` 이 갖는다(모달과 공유). 이 컴포넌트는
 * 페이지 전용 틀(제목·여백)과 성공 후 이동만 맡는다.
 *
 * <p>성공하면 로그인 상태로 대시보드에 도착한다 — 다시 로그인하게 만들지 않는다.
 *
 * <p>`language` 는 페이지에서 `getLanguage()` 로 읽어 props 로 내려받는다 — 이 컴포넌트는
 * `"use client"` 라 쿠키를 직접 못 읽는다.
 */
export function SignupView({ language }: { language: Language }) {
  const t = SIGNUP_TEXT[language];
  const router = useRouter();

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em] text-balance">
        {t.title}
      </h1>
      <div className="mt-3.5">
        <SignupForm language={language} onSuccess={() => router.replace("/app")} />
      </div>
    </div>
  );
}
