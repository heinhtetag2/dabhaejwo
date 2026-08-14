"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import {
  loginFormSchema,
  otpFormSchema,
  useLoginMutation,
  useVerifyOtpMutation,
  type LoginFormValues,
  type OtpChallenge,
  type OtpFormValues,
} from "@/entities/auth/session";
import { ApiError } from "@/shared/api/http-client";
import { ROUTES } from "@/shared/config/routes";
import type { Language } from "@/shared/lib/language";
import { fieldInputClass, FormField } from "@/shared/ui/form-field";

import { LOGIN_TEXT } from "./login-content";

const PRIMARY_BUTTON_CLASS =
  "inline-flex w-full items-center justify-center gap-1.5 rounded-btn border border-[#1c445a] bg-[#1c445a] px-6 py-[14px] text-[15.5px] font-semibold whitespace-nowrap text-white transition-colors hover:border-[#15374a] hover:bg-[#15374a] disabled:cursor-not-allowed disabled:opacity-50";

/**
 * 로그인 폼 — 필드·검증·제출만 담는다(패스워드 단계 / OTP 단계). 제목(h1 vs 모달 제목)은
 * 담는 쪽마다 다르므로 여기 두지 않는다(`login-view.tsx` 페이지, `login-modal.tsx` 모달이
 * 각자 감싼다) — `signup-form.tsx` 와 같은 이유.
 */
export function LoginPasswordForm({
  language,
  onIssued,
  onSwitchToSignup,
}: {
  language: Language;
  onIssued: (challenge: OtpChallenge) => void;
  onSwitchToSignup?: () => void;
}) {
  const t = LOGIN_TEXT[language];
  const router = useRouter();
  const login = useLoginMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginFormSchema(language)),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit((values) => {
    login.mutate(values, {
      onSuccess: onIssued,
      onError: (error) => {
        // 임시 비밀번호로 들어왔다. 재설정 화면으로 보낸다 —
        // 여기서 로그인시키면 메일로 보낸 임시값이 사실상 영구 비밀번호가 된다.
        if (error instanceof ApiError && error.code === "PASSWORD_CHANGE_REQUIRED") {
          router.push(`${ROUTES.forgotPassword}?step=reset&email=${encodeURIComponent(values.email)}`);
        }
      },
    });
  });

  const changeRequired =
    login.error instanceof ApiError && login.error.code === "PASSWORD_CHANGE_REQUIRED";

  return (
    <>
      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="email" label={t.emailLabel} error={errors.email?.message}>
          <input
            id="email"
            type="email"
            autoComplete="username"
            placeholder="you@example.com"
            aria-invalid={errors.email ? true : undefined}
            className={fieldInputClass(!!errors.email)}
            {...register("email")}
          />
        </FormField>

        <FormField id="password" label={t.passwordLabel} error={errors.password?.message}>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            aria-invalid={errors.password ? true : undefined}
            className={fieldInputClass(!!errors.password)}
            {...register("password")}
          />
        </FormField>

        {login.isError && !changeRequired ? (
          <p
            role="alert"
            className="mb-5 rounded-block bg-brick-soft px-4 py-3.5 text-[13.5px] leading-relaxed text-brick"
          >
            {login.error instanceof ApiError && login.error.status === 429
              ? login.error.message
              : t.genericError}
          </p>
        ) : null}

        <button type="submit" disabled={login.isPending} className={PRIMARY_BUTTON_CLASS}>
          {login.isPending ? t.sending : t.next}
        </button>
      </form>

      <p className="mt-6 text-center text-[14px] text-slate">
        <Link
          href={ROUTES.forgotPassword}
          className="font-medium text-slate underline underline-offset-2 hover:text-ink"
        >
          {t.forgotPassword}
        </Link>
      </p>
      <p className="mt-7 text-center text-[14px] text-slate">
        {t.noAccount}{" "}
        {onSwitchToSignup ? (
          <button
            type="button"
            onClick={onSwitchToSignup}
            className="font-semibold text-ink underline underline-offset-2"
          >
            {t.startFree}
          </button>
        ) : (
          <Link href={ROUTES.signup} className="font-semibold text-ink underline underline-offset-2">
            {t.startFree}
          </Link>
        )}
      </p>
      <p className="mt-2.5 text-center text-[13px] leading-relaxed text-slate-2">{t.invitedNote}</p>
    </>
  );
}

export function LoginOtpForm({
  language,
  challenge,
  onBack,
  onSuccess,
}: {
  language: Language;
  challenge: OtpChallenge;
  onBack: () => void;
  onSuccess: () => void;
}) {
  const t = LOGIN_TEXT[language];
  const verify = useVerifyOtpMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<OtpFormValues>({
    resolver: zodResolver(otpFormSchema(language)),
    defaultValues: { code: "" },
  });

  const onSubmit = handleSubmit((values) => {
    verify.mutate({ challengeId: challenge.challengeId, code: values.code }, { onSuccess });
  });

  return (
    <>
      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="otp" label={t.otpLabel} error={errors.code?.message}>
          <input
            id="otp"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            autoFocus
            aria-invalid={errors.code ? true : undefined}
            className={`${fieldInputClass(!!errors.code)} tabular text-center text-[22px] tracking-[0.35em]`}
            {...register("code")}
          />
        </FormField>

        {verify.isError ? (
          <p
            role="alert"
            className="mb-5 rounded-block bg-brick-soft px-4 py-3.5 text-[13.5px] leading-relaxed text-brick"
          >
            {verify.error instanceof ApiError ? verify.error.message : t.otpGenericError}
          </p>
        ) : null}

        <button type="submit" disabled={verify.isPending} className={PRIMARY_BUTTON_CLASS}>
          {verify.isPending ? t.verifying : t.logIn}
        </button>
      </form>

      <p className="mt-6 text-center text-[13.5px] leading-relaxed text-slate-2">
        {t.otpNotReceived}{" "}
        <button
          type="button"
          onClick={onBack}
          className="font-medium text-slate underline underline-offset-2 hover:text-ink"
        >
          {t.otpRetry}
        </button>{" "}
        {t.otpRetrySuffix}
      </p>
    </>
  );
}
