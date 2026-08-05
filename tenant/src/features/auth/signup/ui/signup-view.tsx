"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import {
  signupFormSchema,
  useSignupMutation,
  type SignupFormValues,
} from "@/entities/auth/signup";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import { fieldInputClass, FormField } from "@/shared/ui/form-field";
import { Notice } from "@/shared/ui/notice";

/**
 * 가입.
 *
 * <p><b>한 화면에 끝낸다.</b> 단계를 나누면 이탈한다 (tenant-public-plan.md §4.3).
 * 성공하면 로그인 상태로 대시보드에 도착한다 — 다시 로그인하게 만들지 않는다.
 */
export function SignupView() {
  const router = useRouter();
  const signup = useSignupMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupFormSchema),
    defaultValues: {
      email: "",
      password: "",
      tenantName: "",
      primaryDomain: "",
      termsAgreed: false as unknown as true,
    },
  });

  const onSubmit = handleSubmit((values) => {
    signup.mutate(values, { onSuccess: () => router.replace(ROUTES.home) });
  });

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em] text-balance">
        14일 무료로 시작하기
      </h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">
        카드 등록이 필요 없습니다. 사이트 주소만 알려주시면 바로 학습을 시작합니다.
      </p>

      <form onSubmit={onSubmit} noValidate className="mt-10">
        <FormField id="signup-email" label="이메일" error={errors.email?.message}>
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
          label="비밀번호"
          hint="8자 이상"
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
          label="업체명"
          hint="챗봇 이름의 기본값이 됩니다. 나중에 바꿀 수 있습니다."
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
          label="홈페이지 주소"
          hint="이 주소를 학습하고, 이 주소에서만 챗봇이 뜹니다."
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
              <Link href={ROUTES.terms} className="font-medium underline underline-offset-2">
                이용약관
              </Link>
              과{" "}
              <Link href={ROUTES.privacy} className="font-medium underline underline-offset-2">
                개인정보처리방침
              </Link>
              에 동의합니다.
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
            {signup.error instanceof ApiError
              ? signup.error.message
              : "가입하지 못했습니다. 잠시 후 다시 시도해 주세요."}
          </Notice>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={signup.isPending}
        >
          {signup.isPending ? "만드는 중…" : "무료로 시작하기"}
        </Button>
      </form>

      <p className="mt-7 text-center text-[14px] text-slate">
        이미 계정이 있으신가요?{" "}
        <Link href={ROUTES.login} className="font-semibold text-ink underline underline-offset-2">
          로그인
        </Link>
      </p>
    </div>
  );
}
