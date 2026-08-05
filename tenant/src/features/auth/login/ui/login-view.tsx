"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
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
import { Button } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import { fieldInputClass, FormField } from "@/shared/ui/form-field";

/**
 * 로그인.
 *
 * <p><b>두 단계다.</b> 비밀번호가 맞으면 인증 코드가 메일로 가고, 그 코드를 맞혀야 들어온다.
 * 비밀번호 하나가 새면 계정이 통째로 넘어가는 구조를 없앤다.
 *
 * <p>단계를 라우트로 나누지 않는다 — 새로고침하면 챌린지가 사라지는데, URL 이 남아 있으면
 * 사용자는 되돌아갈 수 있다고 오해한다. 한 화면 안에서 바꾼다.
 */
export function LoginView() {
  const [challenge, setChallenge] = useState<OtpChallenge | null>(null);

  return challenge ? (
    <OtpStep challenge={challenge} onBack={() => setChallenge(null)} />
  ) : (
    <PasswordStep onIssued={setChallenge} />
  );
}

function PasswordStep({ onIssued }: { onIssued: (challenge: OtpChallenge) => void }) {
  const router = useRouter();
  const login = useLoginMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginFormSchema),
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
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">로그인</h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">
        홈페이지에 붙인 챗봇을 여기서 관리합니다.
      </p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="email" label="이메일" error={errors.email?.message}>
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

        <FormField id="password" label="비밀번호" error={errors.password?.message}>
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
              : "이메일 또는 비밀번호가 올바르지 않습니다."}
          </p>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={login.isPending}
        >
          {login.isPending ? "인증 코드를 보내는 중…" : "다음"}
        </Button>
      </form>

      <p className="mt-6 text-center text-[14px] text-slate">
        <Link
          href={ROUTES.forgotPassword}
          className="font-medium text-slate underline underline-offset-2 hover:text-ink"
        >
          비밀번호를 잊으셨나요?
        </Link>
      </p>
      <p className="mt-7 text-center text-[14px] text-slate">
        아직 계정이 없으신가요?{" "}
        <Link href={ROUTES.signup} className="font-semibold text-ink underline underline-offset-2">
          무료로 시작하기
        </Link>
      </p>
      <p className="mt-2.5 text-center text-[13px] leading-relaxed text-slate-2">
        팀원으로 초대받으셨다면 초대 메일의 링크로 들어와 주세요.
      </p>
    </div>
  );
}

function OtpStep({ challenge, onBack }: { challenge: OtpChallenge; onBack: () => void }) {
  const router = useRouter();
  const verify = useVerifyOtpMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<OtpFormValues>({
    resolver: zodResolver(otpFormSchema),
    defaultValues: { code: "" },
  });

  const onSubmit = handleSubmit((values) => {
    verify.mutate(
      { challengeId: challenge.challengeId, code: values.code },
      { onSuccess: () => router.replace(ROUTES.home) },
    );
  });

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">인증 코드 입력</h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">
        <b className="font-semibold text-ink">{challenge.maskedEmail}</b> 으로 6자리 코드를
        보냈습니다. {challenge.ttlMinutes}분 안에 입력해 주세요.
      </p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="otp" label="인증 코드" error={errors.code?.message}>
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
            {verify.error instanceof ApiError
              ? verify.error.message
              : "인증하지 못했습니다. 잠시 후 다시 시도해 주세요."}
          </p>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={verify.isPending}
        >
          {verify.isPending ? "확인 중…" : "로그인"}
        </Button>
      </form>

      <p className="mt-6 text-center text-[13.5px] leading-relaxed text-slate-2">
        메일이 오지 않았나요? 스팸함을 확인해 보시고,{" "}
        <button
          type="button"
          onClick={onBack}
          className="font-medium text-slate underline underline-offset-2 hover:text-ink"
        >
          처음부터 다시
        </button>{" "}
        시도해 주세요.
      </p>
    </div>
  );
}
