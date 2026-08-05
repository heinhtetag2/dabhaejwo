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
import { fieldInputClass, FormField } from "@/shared/ui/form-field";
import { Notice } from "@/shared/ui/notice";

/**
 * 비밀번호 찾기.
 *
 * <p>이메일 입력 → 임시 비밀번호 발송 → 임시 비밀번호로 본인 확인 → 새 비밀번호 설정.
 *
 * <p>단계를 URL 로 남긴다({@code ?step=reset}) — 메일을 확인하러 갔다가 돌아오는 흐름이라
 * 상태를 메모리에만 두면 탭을 옮기는 순간 처음으로 돌아간다.
 */
export function PasswordResetView() {
  const params = useSearchParams();
  const [step, setStep] = useState<"forgot" | "reset">(
    params.get("step") === "reset" ? "reset" : "forgot",
  );
  const [email, setEmail] = useState(params.get("email") ?? "");

  return step === "forgot" ? (
    <ForgotStep
      onSent={(sentTo) => {
        setEmail(sentTo);
        setStep("reset");
      }}
    />
  ) : (
    <ResetStep email={email} />
  );
}

function ForgotStep({ onSent }: { onSent: (email: string) => void }) {
  const forgot = useForgotPasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotFormValues>({
    resolver: zodResolver(forgotFormSchema),
    defaultValues: { email: "" },
  });

  const onSubmit = handleSubmit((values) => {
    forgot.mutate(values, { onSuccess: () => onSent(values.email) });
  });

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">비밀번호 찾기</h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">
        가입하신 이메일로 임시 비밀번호를 보내드립니다.
      </p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="forgot-email" label="이메일" error={errors.email?.message}>
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
            {forgot.error instanceof ApiError
              ? forgot.error.message
              : "메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요."}
          </Notice>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={forgot.isPending}
        >
          {forgot.isPending ? "보내는 중…" : "임시 비밀번호 받기"}
        </Button>
      </form>

      {/* 가입 여부를 알려주지 않는다. 서버도 같은 이유로 없는 주소에 204 를 준다. */}
      <p className="mt-6 text-center text-[13px] leading-relaxed text-slate-2">
        가입된 주소라면 메일이 갑니다. 오지 않으면 스팸함을 확인해 주세요.
      </p>
      <p className="mt-7 text-center text-[14px] text-slate">
        <Link href={ROUTES.login} className="font-medium underline underline-offset-2">
          로그인으로 돌아가기
        </Link>
      </p>
    </div>
  );
}

function ResetStep({ email }: { email: string }) {
  const router = useRouter();
  const reset = useResetPasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetFormValues>({
    resolver: zodResolver(resetFormSchema),
    defaultValues: { email, temporaryPassword: "", newPassword: "", confirmPassword: "" },
  });

  const onSubmit = handleSubmit((values) => {
    reset.mutate(values, {
      onSuccess: () => router.replace(`${ROUTES.login}?reset=done`),
    });
  });

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">새 비밀번호 설정</h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">
        메일로 받은 임시 비밀번호를 입력하고 새 비밀번호를 정해 주세요.
      </p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="reset-email" label="이메일" error={errors.email?.message}>
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
          label="임시 비밀번호"
          hint="메일에 적힌 값을 그대로 입력해 주세요."
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
          label="새 비밀번호"
          hint="8자 이상"
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

        <FormField
          id="reset-confirm"
          label="새 비밀번호 확인"
          error={errors.confirmPassword?.message}
        >
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
            {reset.error instanceof ApiError
              ? reset.error.message
              : "비밀번호를 바꾸지 못했습니다. 잠시 후 다시 시도해 주세요."}
          </Notice>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={reset.isPending}
        >
          {reset.isPending ? "바꾸는 중…" : "비밀번호 바꾸기"}
        </Button>
      </form>

      <p className="mt-7 text-center text-[14px] text-slate">
        <Link href={ROUTES.login} className="font-medium underline underline-offset-2">
          로그인으로 돌아가기
        </Link>
      </p>
    </div>
  );
}
