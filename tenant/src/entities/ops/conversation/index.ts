"use client";

import { useQuery } from "@tanstack/react-query";
import { z } from "zod";

import { api, type PageResponse } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/** 대화 로그. 키는 api-contracts.md §9-3 과 동일하다. */
export type MessageRole = "VISITOR" | "BOT";

export interface ConversationSummary {
  id: string;
  visitorRegion: string | null;
  startedPath: string | null;
  startedAt: string;
  /** 첫 방문자 발화. 열기만 하고 닫은 대화는 null 이다. */
  preview: string | null;
  hasFailure: boolean;
}

export interface ConversationMessage {
  id: string;
  role: MessageRole;
  content: string;
  /** BOT 메시지만 값이 있다. false 면 답변 실패다. */
  answered: boolean | null;
  saved: boolean;
  createdAt: string;
}

export interface ConversationDetail extends Omit<ConversationSummary, "preview" | "hasFailure"> {
  endedAt: string | null;
  messages: ConversationMessage[];
}

const summarySchema = z.object({
  id: z.string(),
  visitorRegion: z.string().nullable(),
  startedPath: z.string().nullable(),
  startedAt: z.string(),
  preview: z.string().nullable(),
  hasFailure: z.boolean(),
});

const summaryPageSchema = z.object({
  content: z.array(summarySchema),
  page: z.object({
    number: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  }),
});

const detailSchema = z.object({
  id: z.string(),
  visitorRegion: z.string().nullable(),
  startedPath: z.string().nullable(),
  startedAt: z.string(),
  endedAt: z.string().nullable(),
  messages: z.array(
    z.object({
      id: z.string(),
      role: z.enum(["VISITOR", "BOT"]),
      content: z.string(),
      answered: z.boolean().nullable(),
      saved: z.boolean(),
      createdAt: z.string(),
    }),
  ),
});

export const conversationKeys = {
  list: (q: string, page: number) => ["conversation", "list", q, page] as const,
  detail: (id: string) => ["conversation", "detail", id] as const,
};

export function useConversationsQuery(q: string, page: number) {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<PageResponse<ConversationSummary>>({
    queryKey: conversationKeys.list(q, page),
    enabled: accessToken !== null,
    queryFn: async () =>
      summaryPageSchema.parse(await api("/api/app/conversations", { query: { q, page } })),
  });
}

export function useConversationDetailQuery(id: string | null) {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<ConversationDetail>({
    queryKey: conversationKeys.detail(id ?? ""),
    enabled: accessToken !== null && id !== null,
    queryFn: async () => detailSchema.parse(await api(`/api/app/conversations/${id}`)),
  });
}
