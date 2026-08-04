"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { ApiError, api } from "@/shared/api/http-client";
import { env } from "@/shared/config/env";
import { currentAccessToken } from "@/shared/lib/auth-store";

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

/**
 * 파일 업로드.
 *
 * <p>`api` 헬퍼는 JSON 전용이라 여기서는 fetch 를 직접 쓴다. multipart 는 브라우저가
 * 경계 문자열을 만들어야 하므로 <b>Content-Type 을 우리가 정하면 안 된다.</b>
 */
export function useUploadDocument() {
  const invalidate = useInvalidateKnowledge();

  return useMutation({
    mutationFn: async (file: File) => {
      const form = new FormData();
      form.append("file", file);

      const response = await fetch(new URL("/api/app/knowledge/documents", env.apiBaseUrl), {
        method: "POST",
        headers: { Authorization: `Bearer ${currentAccessToken() ?? ""}` },
        body: form,
      });
      if (!response.ok) {
        const body = (await response.json().catch(() => null)) as
          | { code?: string; message?: string }
          | null;
        throw new ApiError(response.status, {
          code: body?.code ?? "UNKNOWN",
          message: body?.message ?? "업로드하지 못했습니다",
        });
      }
      return response.json();
    },
    onSuccess: invalidate,
  });
}

export function useDeleteDocument() {
  const invalidate = useInvalidateKnowledge();
  return useMutation({
    mutationFn: (id: string) =>
      api(`/api/app/knowledge/documents/${id}`, { method: "DELETE" }),
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
