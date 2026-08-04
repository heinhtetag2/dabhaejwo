"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/** 가입. 응답 형태는 로그인과 같다 — 가입 직후 로그인 상태여야 한다 (§7.2). */
export const signupFormSchema = z.object({
  email: z.email("이메일 형식이 아닙니다"),
  password: z.string().min(8, "8자 이상이어야 합니다"),
  tenantName: z.string().trim().min(1, "업체명을 입력하세요").max(60),
  primaryDomain: z
    .string()
    .trim()
    .min(1, "홈페이지 주소를 입력하세요")
    .refine((value) => value.replace(/^https?:\/\//, "").split("/")[0].includes("."), {
      message: "주소 형식이 올바르지 않습니다. 예: shop.example.com",
    }),
  termsAgreed: z.literal(true, { message: "약관에 동의해야 가입할 수 있습니다" }),
});

export type SignupFormValues = z.infer<typeof signupFormSchema>;

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
