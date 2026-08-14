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
import type { Language } from "@/shared/lib/language";
import { fieldInputClass, FormField } from "@/shared/ui/form-field";
import { Notice } from "@/shared/ui/notice";

import { INVITE_TEXT } from "./invite-content";

/**
 * 초대 수락.
 *
 * <p>비밀번호를 정하기 전에 <b>어디에 초대됐는지 먼저 보여준다</b> — 모르는 업체 이름이
 * 뜨면 잘못 온 것이고, 그걸 비밀번호를 만든 뒤에 알면 늦다.
 *
 * <p>끝나면 로그인 화면으로 보낸다. 여기서 바로 로그인시키지 않는 이유는 로그인이
 * 2단계이기 때문이다 — 방금 정한 비밀번호로 한 번 들어가 보는 편이 확실하다.
 *
 * <p>`language` 는 페이지에서 `getLanguage()` 로 읽어 props 로 내려받는다 — 이 컴포넌트는
 * `"use client"` 라 쿠키를 직접 못 읽는다.
 */
export function InviteAcceptView({ language }: { language: Language }) {
  const t = INVITE_TEXT[language];
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
    resolver: zodResolver(inviteAcceptFormSchema(language)),
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
        <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em]">{t.invalidTitle}</h1>
        <Notice tone="warn" size="md" className="mt-7">
          {t.invalidNotice}
        </Notice>
        <p className="mt-7 text-center text-[14px] text-slate">
          <Link href={ROUTES.login} className="font-medium underline underline-offset-2">
            {t.backToLogin}
          </Link>
        </p>
      </div>
    );
  }

  if (preview.isPending) {
    return (
      <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
        <p className="text-[15px] text-slate">{t.checking}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-105 px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em] text-balance">
        {t.invitedTitle(preview.data.tenantName)}
      </h1>
      <p className="mt-3.5 text-[15.5px] leading-[1.7] text-slate">{t.subtitle}</p>

      <dl className="mt-8 space-y-2.5 rounded-block bg-fill px-5 py-4 text-[14px]">
        <div className="flex justify-between gap-4">
          <dt className="text-slate">{t.emailLabel}</dt>
          <dd className="min-w-0 truncate font-medium">{preview.data.email}</dd>
        </div>
        <div className="flex justify-between gap-4">
          <dt className="text-slate">{t.roleFieldLabel}</dt>
          <dd className="font-medium">{t.roleLabel[preview.data.role] ?? preview.data.role}</dd>
        </div>
      </dl>

      <form onSubmit={onSubmit} noValidate className="mt-8">
        <FormField
          id="invite-password"
          label={t.passwordLabel}
          hint={t.passwordHint}
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

        <FormField id="invite-confirm" label={t.confirmPasswordLabel} error={errors.confirmPassword?.message}>
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
            {accept.error instanceof ApiError ? accept.error.message : t.genericError}
          </Notice>
        ) : null}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full justify-center"
          disabled={accept.isPending}
        >
          {accept.isPending ? t.settingUp : t.setPasswordAndStart}
        </Button>
      </form>
    </div>
  );
}
