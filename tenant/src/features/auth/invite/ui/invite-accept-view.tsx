"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";

import {
  inviteAcceptFormSchema,
  useAcceptInviteMutation,
  useInvitePreviewQueryFn,
  type InviteAcceptFormValues,
} from "@/entities/auth/session";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import { fieldInputClass, FormField } from "@/shared/ui/form-field";
import { Notice } from "@/shared/ui/notice";

const ROLE_LABEL: Record<string, string> = {
  OWNER: "소유자",
  EDITOR: "편집",
  VIEWER: "보기만",
};

/**
 * 초대 수락.
 *
 * <p>비밀번호를 정하기 전에 <b>어디에 초대됐는지 먼저 보여준다</b> — 모르는 업체 이름이
 * 뜨면 잘못 온 것이고, 그걸 비밀번호를 만든 뒤에 알면 늦다.
 *
 * <p>끝나면 로그인 화면으로 보낸다. 여기서 바로 로그인시키지 않는 이유는 로그인이
 * 2단계이기 때문이다 — 방금 정한 비밀번호로 한 번 들어가 보는 편이 확실하다.
 */
export function InviteAcceptView() {
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const router = useRouter();

  const fetchPreview = useInvitePreviewQueryFn();
  const preview = useQuery({
    queryKey: ["invite", "preview", token],
    enabled: token.length > 0,
    retry: false,
    queryFn: () => fetchPreview(token),
  });

  const accept = useAcceptInviteMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<InviteAcceptFormValues>({
    resolver: zodResolver(inviteAcceptFormSchema),
    defaultValues: { password: "", confirmPassword: "" },
  });

  const onSubmit = handleSubmit((values) => {
    accept.mutate(
      { token, password: values.password },
      { onSuccess: () => router.replace(`${ROUTES.login}?invited=done`) },
    );
  });

  if (!token || preview.isError) {
    return (
      <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
        <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">
          초대 링크를 열 수 없습니다
        </h1>
        <Notice tone="warn" size="md" className="mt-7">
          링크가 만료되었거나 이미 사용되었습니다. 초대한 분께 다시 보내달라고 요청해 주세요.
        </Notice>
        <p className="mt-7 text-center text-[14px] text-slate">
          <Link href={ROUTES.login} className="font-medium underline underline-offset-2">
            로그인으로 돌아가기
          </Link>
        </p>
      </div>
    );
  }

  if (preview.isPending) {
    return (
      <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
        <p className="text-[15px] text-slate">초대를 확인하는 중…</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em] text-balance">
        {preview.data.tenantName} 팀에 초대되었습니다
      </h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">
        비밀번호를 정하시면 바로 시작할 수 있습니다.
      </p>

      <dl className="mt-8 space-y-2.5 rounded-block bg-fill px-5 py-4 text-[14px]">
        <div className="flex justify-between gap-4">
          <dt className="text-slate">이메일</dt>
          <dd className="min-w-0 truncate font-medium">{preview.data.email}</dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-slate">권한</dt>
          <dd className="font-medium">{ROLE_LABEL[preview.data.role] ?? preview.data.role}</dd>
        </div>
      </dl>

      <form onSubmit={onSubmit} noValidate className="mt-8">
        <FormField
          id="invite-password"
          label="비밀번호"
          hint="8자 이상"
          error={errors.password?.message}
        >
          <input
            id="invite-password"
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.password ? true : undefined}
            className={fieldInputClass(!!errors.password)}
            {...register("password")}
          />
        </FormField>

        <FormField
          id="invite-confirm"
          label="비밀번호 확인"
          error={errors.confirmPassword?.message}
        >
          <input
            id="invite-confirm"
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.confirmPassword ? true : undefined}
            className={fieldInputClass(!!errors.confirmPassword)}
            {...register("confirmPassword")}
          />
        </FormField>

        {accept.isError ? (
          <Notice tone="error" size="md" className="mb-5">
            {accept.error instanceof ApiError
              ? accept.error.message
              : "비밀번호를 설정하지 못했습니다. 잠시 후 다시 시도해 주세요."}
          </Notice>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={accept.isPending}
        >
          {accept.isPending ? "설정하는 중…" : "비밀번호 설정하고 시작하기"}
        </Button>
      </form>
    </div>
  );
}
