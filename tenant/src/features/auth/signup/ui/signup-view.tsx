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
    <div className="mx-auto max-w-[440px] px-5 pt-14 pb-10">
      <h1 className="text-[26px] font-semibold tracking-[-0.03em]">14일 무료로 시작하기</h1>
      <p className="mt-2.5 text-[13.5px] leading-relaxed text-slate">
        카드 등록이 필요 없습니다. 사이트 주소만 알려주시면 바로 학습을 시작합니다.
      </p>

      <form
        onSubmit={onSubmit}
        noValidate
        className="mt-7 rounded-card border border-line bg-card p-6"
      >
        <Field id="signup-email" label="이메일" error={errors.email?.message}>
          <input
            id="signup-email"
            type="email"
            autoComplete="username"
            aria-invalid={errors.email ? true : undefined}
            className={INPUT}
            {...register("email")}
          />
        </Field>

        <Field
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
            className={INPUT}
            {...register("password")}
          />
        </Field>

        <Field
          id="signup-tenant"
          label="업체명"
          hint="챗봇 이름의 기본값이 됩니다. 나중에 바꿀 수 있습니다."
          error={errors.tenantName?.message}
        >
          <input
            id="signup-tenant"
            aria-invalid={errors.tenantName ? true : undefined}
            className={INPUT}
            {...register("tenantName")}
          />
        </Field>

        <Field
          id="signup-domain"
          label="홈페이지 주소"
          hint="이 주소를 학습하고, 이 주소에서만 챗봇이 뜹니다."
          error={errors.primaryDomain?.message}
        >
          <input
            id="signup-domain"
            placeholder="shop.example.com"
            aria-invalid={errors.primaryDomain ? true : undefined}
            className={INPUT}
            {...register("primaryDomain")}
          />
        </Field>

        <div className="mb-[18px]">
          <label className="flex items-start gap-2 text-[13px]">
            <input type="checkbox" className="mt-1 size-3.5" {...register("termsAgreed")} />
            <span>
              <Link href={ROUTES.terms} className="underline">
                이용약관
              </Link>
              과{" "}
              <Link href={ROUTES.privacy} className="underline">
                개인정보처리방침
              </Link>
              에 동의합니다.
            </span>
          </label>
          {errors.termsAgreed ? (
            <p role="alert" className="mt-1.5 text-[11.5px] text-brick">
              {errors.termsAgreed.message}
            </p>
          ) : null}
        </div>

        {signup.isError ? (
          <Notice tone="error" className="mb-[18px]">
            {signup.error instanceof ApiError
              ? signup.error.message
              : "가입하지 못했습니다. 잠시 후 다시 시도해 주세요."}
          </Notice>
        ) : null}

        <Button type="submit" variant="accent" className="w-full justify-center" disabled={signup.isPending}>
          {signup.isPending ? "만드는 중…" : "무료로 시작하기"}
        </Button>
      </form>

      <p className="mt-4 text-center text-[12.5px] text-slate-2">
        이미 계정이 있으신가요?{" "}
        <Link href={ROUTES.login} className="underline">
          로그인
        </Link>
      </p>
    </div>
  );
}

const INPUT =
  "w-full rounded-[7px] border border-line bg-card px-[11px] py-[8.5px] text-[13.5px] focus:border-ink-3 focus:outline-none";

function Field({
  id,
  label,
  hint,
  error,
  children,
}: {
  id: string;
  label: string;
  hint?: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mb-[18px]">
      <label htmlFor={id} className="mb-1.5 block text-[12.5px] font-medium">
        {label}
      </label>
      {children}
      {error ? (
        <p role="alert" className="mt-1.5 text-[11.5px] text-brick">
          {error}
        </p>
      ) : hint ? (
        <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">{hint}</p>
      ) : null}
    </div>
  );
}
