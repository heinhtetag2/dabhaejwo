import { useQuery } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";

import type { BillingRecordItem, BillingStatus, MonthlyRevenue, RevenueSummary } from "./types";

export const revenueKeys = {
  all: ["revenue"] as const,
  summary: () => [...revenueKeys.all, "summary"] as const,
  monthly: (months: number) => [...revenueKeys.all, "monthly", months] as const,
  records: (params: { period?: string; status?: BillingStatus | null; page: number }) =>
    [...revenueKeys.all, "records", params] as const,
};

export function useRevenueSummaryQuery() {
  return useQuery({
    queryKey: revenueKeys.summary(),
    queryFn: () => api<RevenueSummary>("/api/ops/revenue/summary"),
  });
}

export function useMonthlyRevenueQuery(months = 12) {
  return useQuery({
    queryKey: revenueKeys.monthly(months),
    queryFn: () => api<MonthlyRevenue[]>("/api/ops/revenue/monthly", { query: { months } }),
  });
}

export function useBillingRecordsQuery(params: {
  period?: string;
  status?: BillingStatus | null;
  page?: number;
}) {
  const page = params.page ?? 0;
  return useQuery({
    queryKey: revenueKeys.records({ period: params.period, status: params.status, page }),
    queryFn: () =>
      api<PageResponse<BillingRecordItem>>("/api/ops/revenue/records", {
        query: { period: params.period, status: params.status, page },
      }),
  });
}
