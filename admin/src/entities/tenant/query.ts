import { useQuery } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";

import type {
  OpsBot,
  QuotaOverride,
  TenantActivity,
  TenantDetail,
  TenantFilterCounts,
  TenantListParams,
  TenantNote,
  TenantSummary,
} from "./types";

export const tenantKeys = {
  all: ["tenant"] as const,
  list: (params: TenantListParams) => [...tenantKeys.all, "list", params] as const,
  filters: () => [...tenantKeys.all, "filters"] as const,
  detail: (id: string) => [...tenantKeys.all, "detail", id] as const,
  activities: (id: string) => [...tenantKeys.all, "activities", id] as const,
  notes: (id: string) => [...tenantKeys.all, "notes", id] as const,
  quotas: (id: string) => [...tenantKeys.all, "quotas", id] as const,
};

export function useTenantListQuery(params: TenantListParams) {
  return useQuery({
    queryKey: tenantKeys.list(params),
    queryFn: () =>
      api<PageResponse<TenantSummary>>("/api/ops/tenants", {
        query: {
          q: params.q,
          filter: params.filter ?? "ALL",
          // 기본 정렬이 원가율 내림차순인 이유는 운영자가 매일 가장 먼저 확인해야 할
          // 대상이 손실 계정이기 때문이다 (admin-console-tenant-plan.md §4.1.3)
          sort: params.sort ?? "COST_RATIO_DESC",
          page: params.page ?? 0,
          size: params.size,
        },
      }),
  });
}

export function useTenantFilterCountsQuery() {
  return useQuery({
    queryKey: tenantKeys.filters(),
    queryFn: () => api<TenantFilterCounts>("/api/ops/tenants/filters"),
  });
}

export function useTenantDetailQuery(id: string | null) {
  return useQuery({
    queryKey: tenantKeys.detail(id ?? ""),
    queryFn: () => api<TenantDetail>(`/api/ops/tenants/${id}`),
    enabled: Boolean(id),
  });
}

export function useTenantActivitiesQuery(id: string | null) {
  return useQuery({
    queryKey: tenantKeys.activities(id ?? ""),
    queryFn: () => api<PageResponse<TenantActivity>>(`/api/ops/tenants/${id}/activities`),
    enabled: Boolean(id),
  });
}

export function useTenantNotesQuery(id: string | null) {
  return useQuery({
    queryKey: tenantKeys.notes(id ?? ""),
    queryFn: () => api<TenantNote[]>(`/api/ops/tenants/${id}/notes`),
    enabled: Boolean(id),
  });
}

export function useQuotaOverridesQuery(id: string | null) {
  return useQuery({
    queryKey: tenantKeys.quotas(id ?? ""),
    queryFn: () => api<QuotaOverride[]>(`/api/ops/tenants/${id}/quota-overrides`),
    enabled: Boolean(id),
  });
}

/** 업체가 가진 서비스. 허용 주소를 함께 준다 — CS 문의 1순위가 미등록 주소다. */
export function useTenantBotsQuery(tenantId: string) {
  return useQuery<OpsBot[]>({
    queryKey: [...tenantKeys.detail(tenantId), "bots"],
    queryFn: () => api<OpsBot[]>(`/api/ops/tenants/${tenantId}/bots`),
  });
}
