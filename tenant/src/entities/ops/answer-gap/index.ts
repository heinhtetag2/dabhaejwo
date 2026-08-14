"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api, type PageResponse } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";
import { botApi, botKey, useCurrentBotId } from "@/shared/lib/current-bot";

/** 답변 개선 대상. 키는 api-contracts.md §9-3 과 동일하다. */
export type GapReason = "ANSWER_FAILED" | "THUMBS_DOWN";
export type GapStatus = "OPEN" | "RESOLVED" | "DISMISSED";

export interface AnswerGap {
  id: number;
  question: string;
  reason: GapReason;
  occurrenceCount: number;
  lastAskedAt: string;
  lastPath: string | null;
  botAnswer: string | null;
  status: GapStatus;
}

export const GAP_REASON_LABEL: Record<GapReason, string> = {
  ANSWER_FAILED: "답변 실패",
  THUMBS_DOWN: "👎 받음",
};

const answerGapSchema = z.object({
  id: z.number(),
  question: z.string(),
  reason: z.enum(["ANSWER_FAILED", "THUMBS_DOWN"]),
  occurrenceCount: z.number(),
  lastAskedAt: z.string(),
  lastPath: z.string().nullable(),
  botAnswer: z.string().nullable(),
  status: z.enum(["OPEN", "RESOLVED", "DISMISSED"]),
});

const answerGapPageSchema = z.object({
  content: z.array(answerGapSchema),
  page: z.object({
    number: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  }),
});

export const answerGapKeys = {
  list: (status: GapStatus, page: number) => ["answer-gap", "list", status, page] as const,
};

export function useAnswerGapsQuery(status: GapStatus, page: number) {
  const botId = useCurrentBotId();
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<PageResponse<AnswerGap>>({
    queryKey: botKey(botId, answerGapKeys.list(status, page)),
    enabled: botId !== null && (accessToken !== null),
    queryFn: async () =>
      answerGapPageSchema.parse(
        await api(botApi(botId, "/answer-gaps"), { query: { status, page } }),
      ),
  });
}

function useInvalidateGaps() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: ["answer-gap"] });
    // 답을 등록하면 공통 질문이 생기고 홈의 미답변 건수도 줄어든다.
    void queryClient.invalidateQueries({ queryKey: ["faq"] });
    void queryClient.invalidateQueries({ queryKey: ["home"] });
  };
}

export function useResolveGap() {
  const botId = useCurrentBotId();
  const invalidate = useInvalidateGaps();
  return useMutation({
    mutationFn: ({ id, answer, question }: { id: number; answer: string; question?: string }) =>
      api(botApi(botId, `/answer-gaps/${id}/resolve`), { method: "POST", body: { answer, question } }),
    onSuccess: invalidate,
  });
}

export function useDismissGap() {
  const botId = useCurrentBotId();
  const invalidate = useInvalidateGaps();
  return useMutation({
    mutationFn: (id: number) => api(botApi(botId, `/answer-gaps/${id}/dismiss`), { method: "POST" }),
    onSuccess: invalidate,
  });
}
