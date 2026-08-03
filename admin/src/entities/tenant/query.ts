import { useQuery } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";

import type { TenantDetail, TenantListParams, TenantSummary } from "./types";

export const tenantKeys = {
  all: ["tenant"] as const,
  list: (params: TenantListParams) => [...tenantKeys.all, "list", params] as const,
  detail: (id: string) => [...tenantKeys.all, "detail", id] as const,
};

export function useTenantListQuery(params: TenantListParams) {
  return useQuery({
    queryKey: tenantKeys.list(params),
    queryFn: () =>
      api<PageResponse<TenantSummary>>("/api/ops/tenants", {
        query: {
          q: params.q,
          // 기본 정렬이 원가율 내림차순인 이유는 운영자가 매일 가장 먼저 확인해야 할
          // 대상이 손실 계정이기 때문이다 (tenant-plan.md §4.1.3)
          sort: params.sort ?? "COST_RATIO_DESC",
          page: params.page ?? 0,
          size: params.size,
        },
      }),
  });
}

export function useTenantDetailQuery(id: string | null) {
  return useQuery({
    queryKey: tenantKeys.detail(id ?? ""),
    queryFn: () => api<TenantDetail>(`/api/ops/tenants/${id}`),
    enabled: Boolean(id),
  });
}
