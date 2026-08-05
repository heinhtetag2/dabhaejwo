import { useQuery } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import type {
  AiUsageSummary,
  DailyCost,
  ModelUsage,
  Profitability,
  TopTenantUsage,
} from "./types";

export const usageKeys = {
  all: ["usage"] as const,
  profitability: (page: number) => [...usageKeys.all, "profitability", page] as const,
  summary: () => [...usageKeys.all, "summary"] as const,
  daily: (days: number) => [...usageKeys.all, "daily", days] as const,
  byModel: (periodMonth?: string) => [...usageKeys.all, "by-model", periodMonth ?? ""] as const,
  topTenants: (limit: number) => [...usageKeys.all, "top-tenants", limit] as const,
};

export function useProfitabilityQuery(page = 0) {
  return useQuery({
    queryKey: usageKeys.profitability(page),
    queryFn: () => api<Profitability>("/api/ops/profitability", { query: { page } }),
  });
}

export function useAiUsageSummaryQuery() {
  return useQuery({
    queryKey: usageKeys.summary(),
    queryFn: () => api<AiUsageSummary>("/api/ops/ai-usage/summary"),
  });
}

export function useDailyCostQuery(days = 14) {
  return useQuery({
    queryKey: usageKeys.daily(days),
    queryFn: () => api<DailyCost[]>("/api/ops/ai-usage/daily", { query: { days } }),
  });
}

export function useModelUsageQuery(periodMonth?: string) {
  return useQuery({
    queryKey: usageKeys.byModel(periodMonth),
    queryFn: () => api<ModelUsage[]>("/api/ops/ai-usage/by-model", { query: { periodMonth } }),
  });
}

export function useTopTenantsQuery(limit = 5) {
  return useQuery({
    queryKey: usageKeys.topTenants(limit),
    queryFn: () => api<TopTenantUsage[]>("/api/ops/ai-usage/top-tenants", { query: { limit } }),
  });
}
