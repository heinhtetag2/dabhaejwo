"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { knowledgeDocumentPageSchema, knowledgeSourceListSchema } from "./schema";
import type { DocumentStatus, KnowledgeDocument, KnowledgeSource } from "./types";
import { botApi, botKey, useCurrentBotId } from "@/shared/lib/current-bot";

export const knowledgeKeys = {
  sources: ["knowledge", "list", "sources"] as const,
  documents: (params: DocumentQuery) => ["knowledge", "list", "documents", params] as const,
};

export interface DocumentQuery {
  sourceId?: string;
  status?: DocumentStatus;
  q?: string;
  page: number;
}

export function useKnowledgeSourcesQuery() {
  const botId = useCurrentBotId();
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<KnowledgeSource[]>({
    queryKey: botKey(botId, knowledgeKeys.sources),
    enabled: botId !== null && (accessToken !== null),
    queryFn: async () =>
      knowledgeSourceListSchema.parse(await api(botApi(botId, "/knowledge/sources"))),
  });
}

/** 학습이 진행 중일 때 목록을 다시 읽는 주기. 서버 워커 주기(10초)와 맞춘다. */
const INDEXING_POLL_MS = 5_000;

/**
 * 문서 목록.
 *
 * <p>학습 대기·처리 중 문서가 있으면 <b>스스로 다시 읽는다.</b> 업로드 응답은 항상
 * PENDING 이라, 다시 읽지 않으면 화면은 영원히 "대기 중"이고 업체는 학습이 멈춘 줄 안다.
 * 진행 중인 문서가 없으면 폴링을 멈춘다 — 가만히 있는 화면이 서버를 계속 두드리지 않게.
 */
export function useKnowledgeDocumentsQuery(params: DocumentQuery) {
  const botId = useCurrentBotId();
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<PageResponse<KnowledgeDocument>>({
    refetchInterval: (query) =>
      query.state.data?.content.some(
        (document) => document.status === "PENDING" || document.status === "PROCESSING",
      )
        ? INDEXING_POLL_MS
        : false,
    queryKey: botKey(botId, knowledgeKeys.documents(params)),
    // 소스를 아직 못 고른 상태에서 전체를 긁지 않는다.
    enabled: botId !== null && (accessToken !== null && params.sourceId !== undefined),
    queryFn: async () =>
      knowledgeDocumentPageSchema.parse(
        await api(botApi(botId, "/knowledge/documents"), {
          query: {
            sourceId: params.sourceId,
            status: params.status,
            q: params.q,
            page: params.page,
          },
        }),
      ),
  });
}
