import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { guardKeys } from "@/entities/guard";
import type { ProviderName } from "@/entities/usage";

import type { ProviderCredential } from "./types";

export const providerKeys = {
  all: ["provider-credential"] as const,
  list: () => [...providerKeys.all, "list"] as const,
};

export function useProviderCredentialsQuery() {
  return useQuery({
    queryKey: providerKeys.list(),
    queryFn: () => api<ProviderCredential[]>("/api/ops/provider-credentials"),
  });
}

/** 키 등록·교체. 서버는 저장만 하고 되돌려주지 않는다 — 응답에도 원문이 없다. */
export function useSaveProviderKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      provider,
      apiKey,
      reason,
    }: {
      provider: ProviderName;
      apiKey: string;
      reason: string;
    }) =>
      api<ProviderCredential>(`/api/ops/provider-credentials/${provider}`, {
        method: "PUT",
        body: { apiKey, reason },
      }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: providerKeys.all }),
  });
}

export function useToggleProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      provider,
      enabled,
      reason,
    }: {
      provider: ProviderName;
      enabled: boolean;
      reason: string;
    }) =>
      api<ProviderCredential>(`/api/ops/provider-credentials/${provider}`, {
        method: "PATCH",
        body: { enabled, reason },
      }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: providerKeys.all }),
  });
}

/**
 * 임베딩 공급사 교체.
 *
 * 안전장치 저장과 경로가 다른 이유는 결과가 전혀 다르기 때문이다 —
 * 이 호출은 **이미 학습된 모든 문서를 다시 학습 대상으로 만든다.**
 */
export function useChangeEmbeddingProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ provider, reason }: { provider: ProviderName; reason: string }) =>
      api<unknown>("/api/ops/cost-guards/embedding-provider", {
        method: "PUT",
        body: { provider, reason },
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: guardKeys.all });
      void queryClient.invalidateQueries({ queryKey: providerKeys.all });
    },
  });
}
