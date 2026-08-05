"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import {
  invitePreviewSchema,
  loginResultSchema,
  otpChallengeSchema,
  type ForgotFormValues,
  type InvitePreview,
  type LoginFormValues,
  type OtpChallenge,
  type ResetFormValues,
} from "./schema";
import type { LoginResult } from "./types";

/**
 * 로그인 1단계 — 비밀번호 확인.
 *
 * <p><b>여기서는 로그인되지 않는다.</b> 성공해도 토큰이 없고, 메일로 간 코드를
 * 다음 단계에서 맞혀야 한다. 그래서 {@code signIn} 을 부르지 않는다.
 */
export function useLoginMutation() {
  return useMutation<OtpChallenge, Error, LoginFormValues>({
    mutationFn: async (values) =>
      otpChallengeSchema.parse(
        await api("/api/auth/app/login", { method: "POST", body: values }),
      ),
  });
}

/** 로그인 2단계 — 코드가 맞으면 토큰이 나온다. 여기서만 로그인 상태가 된다. */
export function useVerifyOtpMutation() {
  const queryClient = useQueryClient();
  const signIn = useAuthStore((state) => state.signIn);

  return useMutation<LoginResult, Error, { challengeId: string; code: string }>({
    mutationFn: async (values) =>
      loginResultSchema.parse(
        await api("/api/auth/app/login/otp", { method: "POST", body: values }),
      ),
    onSuccess: (result) => {
      signIn(result.accessToken, result.refreshToken);
      // 이전 계정의 캐시가 남아 다른 업체 데이터가 잠깐 보이는 일이 없게 한다.
      void queryClient.invalidateQueries();
    },
  });
}

/** 임시 비밀번호 발송. 계정이 없어도 성공한다 — 가입 여부를 알려주지 않는다. */
export function useForgotPasswordMutation() {
  return useMutation<void, Error, ForgotFormValues>({
    mutationFn: async (values) => {
      await api("/api/auth/app/password/forgot", { method: "POST", body: values });
    },
  });
}

export function useResetPasswordMutation() {
  return useMutation<void, Error, ResetFormValues>({
    mutationFn: async ({ email, temporaryPassword, newPassword }) => {
      await api("/api/auth/app/password/reset", {
        method: "POST",
        body: { email, temporaryPassword, newPassword },
      });
    },
  });
}

/** 초대 링크를 열었을 때. 비밀번호를 정하기 전에 어디에 초대됐는지 확인시킨다. */
export function useInvitePreviewQueryFn() {
  return async (token: string): Promise<InvitePreview> =>
    invitePreviewSchema.parse(await api("/api/auth/app/invite", { query: { token } }));
}

export function useAcceptInviteMutation() {
  return useMutation<void, Error, { token: string; password: string }>({
    mutationFn: async (values) => {
      await api("/api/auth/app/invite/accept", { method: "POST", body: values });
    },
  });
}
