"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import { loginFormSchema, useLoginMutation, type LoginFormValues } from "@/entities/auth/session";
import { Button } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";

export function LoginView() {
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
    login.mutate(values, { onSuccess: () => router.replace(ROUTES.home) });
  });

  return (
    <main className="flex min-h-dvh items-center justify-center px-4 py-10">
      <div className="w-full max-w-[380px]">
        <div className="mb-7 flex items-center gap-2.5">
          <span className="grid size-[22px] place-items-center rounded-md bg-mark font-mono text-xs font-bold text-ink">
            A
          </span>
          <span className="font-semibold tracking-[-0.01em]">답해줘</span>
        </div>

        <h1 className="text-[19px] font-semibold tracking-[-0.02em]">업체 대시보드</h1>
        <p className="mt-1 text-[12.5px] text-slate-2">
          홈페이지에 붙인 챗봇을 여기서 관리합니다.
        </p>

        <form onSubmit={onSubmit} noValidate className="mt-6 rounded-card border border-line bg-card p-5">
          <div className="mb-[18px]">
            <label htmlFor="email" className="mb-1.5 block text-[12.5px] font-medium">
              이메일
            </label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              aria-invalid={errors.email ? true : undefined}
              className="w-full rounded-[7px] border border-line bg-card px-[11px] py-[8.5px] text-[13.5px] focus:border-ink-3 focus:outline-none"
              {...register("email")}
            />
            {errors.email ? (
              <p role="alert" className="mt-1.5 text-[11.5px] text-brick">
                {errors.email.message}
              </p>
            ) : null}
          </div>

          <div className="mb-[18px]">
            <label htmlFor="password" className="mb-1.5 block text-[12.5px] font-medium">
              비밀번호
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              aria-invalid={errors.password ? true : undefined}
              className="w-full rounded-[7px] border border-line bg-card px-[11px] py-[8.5px] text-[13.5px] focus:border-ink-3 focus:outline-none"
              {...register("password")}
            />
            {errors.password ? (
              <p role="alert" className="mt-1.5 text-[11.5px] text-brick">
                {errors.password.message}
              </p>
            ) : null}
          </div>

          {login.isError ? (
            <p
              role="alert"
              className="mb-[18px] rounded-[7px] bg-brick-soft px-3 py-2.5 text-[12.5px] text-brick"
            >
              이메일 또는 비밀번호가 올바르지 않습니다.
            </p>
          ) : null}

          <Button type="submit" variant="primary" className="w-full" disabled={login.isPending}>
            {login.isPending ? "확인 중…" : "로그인"}
          </Button>
        </form>

        <p className="mt-4 text-center text-[11.5px] text-slate-2">
          계정이 없다면 담당자에게 초대를 요청하세요.
        </p>
      </div>
    </main>
  );
}
