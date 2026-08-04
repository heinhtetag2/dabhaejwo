"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import { faqKeys } from "./query";
import type { FaqSaveInput } from "./types";

/** 어떤 변경이든 목록이 진실이므로 성공하면 목록을 다시 읽는다. */
function useInvalidateFaqs() {
  const queryClient = useQueryClient();
  return () => void queryClient.invalidateQueries({ queryKey: faqKeys.list });
}

export function useCreateFaq() {
  const invalidate = useInvalidateFaqs();
  return useMutation({
    mutationFn: (input: FaqSaveInput) => api("/api/app/faqs", { method: "POST", body: input }),
    onSuccess: invalidate,
  });
}

export function useUpdateFaq() {
  const invalidate = useInvalidateFaqs();
  return useMutation({
    mutationFn: ({ id, ...input }: FaqSaveInput & { id: string }) =>
      api(`/api/app/faqs/${id}`, { method: "PATCH", body: input }),
    onSuccess: invalidate,
  });
}

export function useDeleteFaq() {
  const invalidate = useInvalidateFaqs();
  return useMutation({
    mutationFn: (id: string) => api(`/api/app/faqs/${id}`, { method: "DELETE" }),
    onSuccess: invalidate,
  });
}

/**
 * 순서 변경. 전체 순서를 한 번에 보낸다 — 일부만 보내면 나머지와 순번이 충돌한다.
 */
export function useReorderFaqs() {
  const invalidate = useInvalidateFaqs();
  return useMutation({
    mutationFn: (faqIds: string[]) => api("/api/app/faqs/order", { method: "PATCH", body: { faqIds } }),
    onSuccess: invalidate,
  });
}
