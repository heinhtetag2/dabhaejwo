"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api, type PageResponse } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/** 남긴 연락처. contact 는 마스킹된 값이다 — 원문은 CSV 에서만 나간다. */
export type LeadStatus = "NEW" | "CONTACTED" | "CLOSED";

export interface Lead {
  id: string;
  name: string;
  contact: string;
  reason: string | null;
  status: LeadStatus;
  createdAt: string;
}

export const LEAD_STATUS_LABEL: Record<LeadStatus, string> = {
  NEW: "대기",
  CONTACTED: "연락함",
  CLOSED: "종료",
};

const leadSchema = z.object({
  id: z.string(),
  name: z.string(),
  contact: z.string(),
  reason: z.string().nullable(),
  status: z.enum(["NEW", "CONTACTED", "CLOSED"]),
  createdAt: z.string(),
});

const leadPageSchema = z.object({
  content: z.array(leadSchema),
  page: z.object({
    number: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  }),
});

export const leadKeys = {
  list: (page: number) => ["lead", "list", page] as const,
};

export function useLeadsQuery(page: number) {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<PageResponse<Lead>>({
    queryKey: leadKeys.list(page),
    enabled: accessToken !== null,
    queryFn: async () => leadPageSchema.parse(await api("/api/app/leads", { query: { page } })),
  });
}

export function useChangeLeadStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: LeadStatus }) =>
      api(`/api/app/leads/${id}`, { method: "PATCH", body: { status } }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["lead"] }),
  });
}
