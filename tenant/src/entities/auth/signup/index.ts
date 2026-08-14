"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";
import type { Language } from "@/shared/lib/language";

const VALIDATION_TEXT: Record<
  Language,
  {
    invalidEmail: string;
    passwordMinLength: string;
    tenantNameRequired: string;
    domainRequired: string;
    domainInvalid: string;
    termsRequired: string;
  }
> = {
  en: {
    invalidEmail: "Not a valid email address",
    passwordMinLength: "Must be at least 8 characters",
    tenantNameRequired: "Enter your company name",
    domainRequired: "Enter your site's address",
    domainInvalid: "That doesn't look like a valid address. e.g. shop.example.com",
    termsRequired: "You must agree to the terms to sign up",
  },
  ko: {
    invalidEmail: "이메일 형식이 아닙니다",
    passwordMinLength: "8자 이상이어야 합니다",
    tenantNameRequired: "업체명을 입력하세요",
    domainRequired: "홈페이지 주소를 입력하세요",
    domainInvalid: "주소 형식이 올바르지 않습니다. 예: shop.example.com",
    termsRequired: "약관에 동의해야 가입할 수 있습니다",
  },
};

/** 가입. 응답 형태는 로그인과 같다 — 가입 직후 로그인 상태여야 한다 (§7.2). */
export function signupFormSchema(language: Language) {
  const t = VALIDATION_TEXT[language];
  return z.object({
    email: z.email(t.invalidEmail),
    password: z.string().min(8, t.passwordMinLength),
    tenantName: z.string().trim().min(1, t.tenantNameRequired).max(60),
    primaryDomain: z
      .string()
      .trim()
      .min(1, t.domainRequired)
      .refine((value) => value.replace(/^https?:\/\//, "").split("/")[0].includes("."), {
        message: t.domainInvalid,
      }),
    termsAgreed: z.literal(true, { message: t.termsRequired }),
  });
}

export type SignupFormValues = z.infer<ReturnType<typeof signupFormSchema>>;

const signupResultSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  member: z.object({
    id: z.string(),
    name: z.string().nullable(),
    email: z.string(),
    role: z.enum(["OWNER", "EDITOR", "VIEWER"]),
    inviteState: z.enum(["PENDING", "ACCEPTED"]),
    lastSeenAt: z.string().nullable(),
  }),
});

export function useSignupMutation() {
  const queryClient = useQueryClient();
  const signIn = useAuthStore((state) => state.signIn);

  return useMutation({
    mutationFn: async (values: SignupFormValues) =>
      signupResultSchema.parse(
        await api("/api/auth/app/signup", { method: "POST", body: values }),
      ),
    onSuccess: (result) => {
      signIn(result.accessToken, result.refreshToken);
      void queryClient.invalidateQueries();
    },
  });
}
