"use client";

import { useMutation } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";

/**
 * 미리보기 답변. 위젯의 `/api/widget/ask` 와 <b>같은 파이프라인</b>을 탄다 —
 * 다른 경로로 만들면 미리보기에서 잘 나오던 답이 실제로는 다르게 나온다.
 *
 * <p>대화를 만들지 않아 방문자 통계·답변 개선 목록에 섞이지 않는다.
 * 대신 {@code messageId} 가 없다 — 평가를 붙일 대상이 없다는 뜻이다.
 */
export interface PreviewAnswer {
  answered: boolean;
  saved: boolean;
  answer: string;
  links: string[];
  messageId: string | null;
}

const previewAnswerSchema = z.object({
  answered: z.boolean(),
  saved: z.boolean(),
  answer: z.string(),
  links: z.array(z.string()),
  messageId: z.string().nullable(),
});

export function usePreviewAnswer() {
  return useMutation<PreviewAnswer, unknown, string>({
    mutationFn: async (question: string) =>
      previewAnswerSchema.parse(
        await api("/api/app/chat/preview", { method: "POST", body: { question } }),
      ),
  });
}
