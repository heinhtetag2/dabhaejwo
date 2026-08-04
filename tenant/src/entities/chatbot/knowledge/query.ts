"use client";

import { useQuery } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { knowledgeDocumentPageSchema, knowledgeSourceListSchema } from "./schema";
import type { DocumentStatus, KnowledgeDocument, KnowledgeSource } from "./types";

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
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<KnowledgeSource[]>({
    queryKey: knowledgeKeys.sources,
    enabled: accessToken !== null,
    queryFn: async () =>
      knowledgeSourceListSchema.parse(await api("/api/app/knowledge/sources")),
  });
}

export function useKnowledgeDocumentsQuery(params: DocumentQuery) {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<PageResponse<KnowledgeDocument>>({
    queryKey: knowledgeKeys.documents(params),
    // 소스를 아직 못 고른 상태에서 전체를 긁지 않는다.
    enabled: accessToken !== null && params.sourceId !== undefined,
    queryFn: async () =>
      knowledgeDocumentPageSchema.parse(
        await api("/api/app/knowledge/documents", {
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
