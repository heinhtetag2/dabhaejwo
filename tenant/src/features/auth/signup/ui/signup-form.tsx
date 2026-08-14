"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useForm } from "react-hook-form";

import {
  signupFormSchema,
  useSignupMutation,
  type SignupFormValues,
} from "@/entities/auth/signup";
import { ApiError } from "@/shared/api/http-client";
import { ROUTES } from "@/shared/config/routes";
import type { Language } from "@/shared/lib/language";
import { fieldInputClass, FormField } from "@/shared/ui/form-field";
import { Notice } from "@/shared/ui/notice";

import { SIGNUP_TEXT } from "./signup-content";

/**
 * 가입 폼 — 필드·검증·제출만 담는다. 제목(h1 vs 모달 제목)은 담는 쪽마다 다르므로
 * 여기 두지 않는다(`signup-view.tsx` 페이지, `signup-modal.tsx` 모달이 각자 감싼다).
 *
 * <p><b>한 화면에 끝낸다.</b> 단계를 나누면 이탈한다 (tenant-public-plan.md §4.3) — 모달
 * 안에서도 같은 원칙이라 스텝을 나누지 않는다.
 *
 * <p>{@code onSuccess} 는 호출부가 정한다 — 페이지는 대시보드로 이동, 모달은 모달을 닫고
 * 이동한다. 로그인 상태 전환(`signIn`)은 `useSignupMutation` 안에서 이미 끝난다.
 */
export function SignupForm({
  language,
  onSuccess,
  onSwitchToLogin,
}: {
  language: Language;
  onSuccess: () => void;
  onSwitchToLogin?: () => void;
}) {
  const t = SIGNUP_TEXT[language];
  const signup = useSignupMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupFormSchema(language)),
    defaultValues: {
      email: "",
      password: "",
      tenantName: "",
      primaryDomain: "",
      termsAgreed: false as unknown as true,
    },
  });

  const onSubmit = handleSubmit((values) => {
    signup.mutate(values, { onSuccess });
  });

  return (
    <>
      <p className="text-[15.5px] leading-[1.7] text-slate">{t.subtitle}</p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="signup-email" label={t.emailLabel} error={errors.email?.message}>
          <input
            id="signup-email"
            type="email"
            autoComplete="username"
            placeholder="you@example.com"
            aria-invalid={errors.email ? true : undefined}
            className={fieldInputClass(!!errors.email)}
            {...register("email")}
          />
        </FormField>

        <FormField
          id="signup-password"
          label={t.passwordLabel}
          hint={t.passwordHint}
          error={errors.password?.message}
        >
          <input
            id="signup-password"
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.password ? true : undefined}
            className={fieldInputClass(!!errors.password)}
            {...register("password")}
          />
        </FormField>

        <FormField
          id="signup-tenant"
          label={t.tenantLabel}
          hint={t.tenantHint}
          error={errors.tenantName?.message}
        >
          <input
            id="signup-tenant"
            aria-invalid={errors.tenantName ? true : undefined}
            className={fieldInputClass(!!errors.tenantName)}
            {...register("tenantName")}
          />
        </FormField>

        <FormField
          id="signup-domain"
          label={t.domainLabel}
          hint={t.domainHint}
          error={errors.primaryDomain?.message}
        >
          <input
            id="signup-domain"
            placeholder="shop.example.com"
            aria-invalid={errors.primaryDomain ? true : undefined}
            className={fieldInputClass(!!errors.primaryDomain)}
            {...register("primaryDomain")}
          />
        </FormField>

        <div className="mb-6">
          <label className="flex items-start gap-2.5 rounded-block bg-fill px-4 py-3.5 text-[14px] leading-relaxed">
            <input type="checkbox" className="mt-1 size-4 accent-ink" {...register("termsAgreed")} />
            <span>
              {t.consentBefore}
              <Link href={ROUTES.terms} className="font-medium underline underline-offset-2">
                {t.termsLabel}
              </Link>
              {t.consentMiddle}
              <Link href={ROUTES.privacy} className="font-medium underline underline-offset-2">
                {t.privacyLabel}
              </Link>
              {t.consentAfter}
            </span>
          </label>
          {errors.termsAgreed ? (
            <p role="alert" className="mt-2 text-[12.5px] text-brick">
              {errors.termsAgreed.message}
            </p>
          ) : null}
        </div>

        {signup.isError ? (
          <Notice tone="error" size="md" className="mb-5">
            {signup.error instanceof ApiError ? signup.error.message : t.genericError}
          </Notice>
        ) : null}

        <button
          type="submit"
          disabled={signup.isPending}
          className="inline-flex w-full items-center justify-center gap-1.5 rounded-btn border border-[#1c445a] bg-[#1c445a] px-6 py-[14px] text-[15.5px] font-semibold whitespace-nowrap text-white transition-colors hover:border-[#15374a] hover:bg-[#15374a] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {signup.isPending ? t.creating : t.startFree}
        </button>
      </form>

      <p className="mt-7 text-center text-[14px] text-slate">
        {t.hasAccount}{" "}
        {onSwitchToLogin ? (
          <button
            type="button"
            onClick={onSwitchToLogin}
            className="font-semibold text-ink underline underline-offset-2"
          >
            {t.logIn}
          </button>
        ) : (
          <Link href={ROUTES.login} className="font-semibold text-ink underline underline-offset-2">
            {t.logIn}
          </Link>
        )}
      </p>
    </>
  );
}
