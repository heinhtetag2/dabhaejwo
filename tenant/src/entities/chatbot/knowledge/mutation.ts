"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

/**
 * 소스와 문서는 서로 영향을 준다(제외하면 소스의 문서 수가 바뀐다).
 * 그래서 접두사 하나로 둘 다 무효화한다.
 */
function useInvalidateKnowledge() {
  const queryClient = useQueryClient();
  return () => void queryClient.invalidateQueries({ queryKey: ["knowledge"] });
}

export function useChangeAutoRefresh() {
  const invalidate = useInvalidateKnowledge();
  return useMutation({
    mutationFn: ({ id, autoRefresh }: { id: string; autoRefresh: boolean }) =>
      api(`/api/app/knowledge/sources/${id}`, { method: "PATCH", body: { autoRefresh } }),
    onSuccess: invalidate,
  });
}

export function useChangeExcluded() {
  const invalidate = useInvalidateKnowledge();
  return useMutation({
    mutationFn: ({ id, excluded }: { id: string; excluded: boolean }) =>
      api(`/api/app/knowledge/documents/${id}`, { method: "PATCH", body: { excluded } }),
    onSuccess: invalidate,
  });
}

/**
 * 사이트 다시 읽기 / 다시 학습.
 *
 * 크롤러·임베딩 워커가 붙기 전까지 서버가 FEATURE_NOT_READY 를 돌려준다.
 * 성공한 척하지 않으므로 화면은 그 메시지를 그대로 보여주면 된다.
 */
export function useRecrawlSource() {
  const invalidate = useInvalidateKnowledge();
  return useMutation({
    mutationFn: (id: string) => api(`/api/app/knowledge/sources/${id}/recrawl`, { method: "POST" }),
    onSuccess: invalidate,
  });
}

export function useRetryFailed() {
  const invalidate = useInvalidateKnowledge();
  return useMutation({
    mutationFn: (sourceId?: string) =>
      api("/api/app/knowledge/documents/retry-failed", { method: "POST", query: { sourceId } }),
    onSuccess: invalidate,
  });
}
