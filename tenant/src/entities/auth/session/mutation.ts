"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { loginResultSchema, type LoginFormValues } from "./schema";
import type { LoginResult } from "./types";

export function useLoginMutation() {
  const queryClient = useQueryClient();
  const signIn = useAuthStore((state) => state.signIn);

  return useMutation<LoginResult, Error, LoginFormValues>({
    mutationFn: async (values) =>
      loginResultSchema.parse(
        await api("/api/auth/app/login", { method: "POST", body: values }),
      ),
    onSuccess: (result) => {
      signIn(result.accessToken, result.refreshToken);
      // 이전 계정의 캐시가 남아 다른 업체 데이터가 잠깐 보이는 일이 없게 한다.
      void queryClient.invalidateQueries();
    },
  });
}
