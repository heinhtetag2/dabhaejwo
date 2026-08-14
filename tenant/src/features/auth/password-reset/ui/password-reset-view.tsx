"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import {
  forgotFormSchema,
  resetFormSchema,
  useForgotPasswordMutation,
  useResetPasswordMutation,
  type ForgotFormValues,
  type ResetFormValues,
} from "@/entities/auth/session";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import type { Language } from "@/shared/lib/language";
import { fieldInputClass, FormField } from "@/shared/ui/form-field";
import { Notice } from "@/shared/ui/notice";

import { PASSWORD_RESET_TEXT } from "./password-reset-content";

/**
 * 비밀번호 찾기.
 *
 * <p>이메일 입력 → 임시 비밀번호 발송 → 임시 비밀번호로 본인 확인 → 새 비밀번호 설정.
 *
 * <p>단계를 URL 로 남긴다({@code ?step=reset}) — 메일을 확인하러 갔다가 돌아오는 흐름이라
 * 상태를 메모리에만 두면 탭을 옮기는 순간 처음으로 돌아간다.
 *
 * <p>`language` 는 페이지에서 `getLanguage()` 로 읽어 props 로 내려받는다 — 이 컴포넌트는
 * `"use client"` 라 쿠키를 직접 못 읽는다.
 */
export function PasswordResetView({ language }: { language: Language }) {
  const params = useSearchParams();
  const [step, setStep] = useState<"forgot" | "reset">(
    params.get("step") === "reset" ? "reset" : "forgot",
  );
  const [email, setEmail] = useState(params.get("email") ?? "");

  return step === "forgot" ? (
    <ForgotStep
      language={language}
      onSent={(sentTo) => {
        setEmail(sentTo);
        setStep("reset");
      }}
    />
  ) : (
    <ResetStep email={email} language={language} />
  );
}

function ForgotStep({ onSent, language }: { onSent: (email: string) => void; language: Language }) {
  const t = PASSWORD_RESET_TEXT[language];
  const forgot = useForgotPasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotFormValues>({
    resolver: zodResolver(forgotFormSchema(language)),
    defaultValues: { email: "" },
  });

  const onSubmit = handleSubmit((values) => {
    forgot.mutate(values, { onSuccess: () => onSent(values.email) });
  });

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">{t.forgotTitle}</h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">{t.forgotSubtitle}</p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="forgot-email" label={t.emailLabel} error={errors.email?.message}>
          <input
            id="forgot-email"
            type="email"
            autoComplete="username"
            placeholder="you@example.com"
            aria-invalid={errors.email ? true : undefined}
            className={fieldInputClass(!!errors.email)}
            {...register("email")}
          />
        </FormField>

        {forgot.isError ? (
          <Notice tone="error" size="md" className="mb-5">
            {forgot.error instanceof ApiError ? forgot.error.message : t.forgotGenericError}
          </Notice>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={forgot.isPending}
        >
          {forgot.isPending ? t.sending : t.getTemporaryPassword}
        </Button>
      </form>

      {/* 가입 여부를 알려주지 않는다. 서버도 같은 이유로 없는 주소에 204 를 준다. */}
      <p className="mt-6 text-center text-[13px] leading-relaxed text-slate-2">{t.forgotNote}</p>
      <p className="mt-7 text-center text-[14px] text-slate">
        <Link href={ROUTES.login} className="font-medium underline underline-offset-2">
          {t.backToLogin}
        </Link>
      </p>
    </div>
  );
}

function ResetStep({ email, language }: { email: string; language: Language }) {
  const t = PASSWORD_RESET_TEXT[language];
  const router = useRouter();
  const reset = useResetPasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetFormValues>({
    resolver: zodResolver(resetFormSchema(language)),
    defaultValues: { email, temporaryPassword: "", newPassword: "", confirmPassword: "" },
  });

  const onSubmit = handleSubmit((values) => {
    reset.mutate(values, {
      onSuccess: () => router.replace(`${ROUTES.login}?reset=done`),
    });
  });

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">{t.resetTitle}</h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">{t.resetSubtitle}</p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="reset-email" label={t.emailLabel} error={errors.email?.message}>
          <input
            id="reset-email"
            type="email"
            autoComplete="username"
            aria-invalid={errors.email ? true : undefined}
            className={fieldInputClass(!!errors.email)}
            {...register("email")}
          />
        </FormField>

        <FormField
          id="reset-temp"
          label={t.temporaryPasswordLabel}
          hint={t.temporaryPasswordHint}
          error={errors.temporaryPassword?.message}
        >
          <input
            id="reset-temp"
            autoComplete="one-time-code"
            aria-invalid={errors.temporaryPassword ? true : undefined}
            className={`${fieldInputClass(!!errors.temporaryPassword)} font-mono`}
            {...register("temporaryPassword")}
          />
        </FormField>

        <FormField
          id="reset-new"
          label={t.newPasswordLabel}
          hint={t.newPasswordHint}
          error={errors.newPassword?.message}
        >
          <input
            id="reset-new"
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.newPassword ? true : undefined}
            className={fieldInputClass(!!errors.newPassword)}
            {...register("newPassword")}
          />
        </FormField>

        <FormField id="reset-confirm" label={t.confirmPasswordLabel} error={errors.confirmPassword?.message}>
          <input
            id="reset-confirm"
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.confirmPassword ? true : undefined}
            className={fieldInputClass(!!errors.confirmPassword)}
            {...register("confirmPassword")}
          />
        </FormField>

        {reset.isError ? (
          <Notice tone="error" size="md" className="mb-5">
            {reset.error instanceof ApiError ? reset.error.message : t.resetGenericError}
          </Notice>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={reset.isPending}
        >
          {reset.isPending ? t.changing : t.changePassword}
        </Button>
      </form>

      <p className="mt-7 text-center text-[14px] text-slate">
        <Link href={ROUTES.login} className="font-medium underline underline-offset-2">
          {t.backToLogin}
        </Link>
      </p>
    </div>
  );
}
