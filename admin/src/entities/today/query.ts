import { useQuery } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import type { TodaySummary } from "./types";

export const todayKeys = {
  all: ["today"] as const,
  summary: () => [...todayKeys.all, "summary"] as const,
};

export function useTodayQuery() {
  return useQuery({
    queryKey: todayKeys.summary(),
    queryFn: () => api<TodaySummary>("/api/ops/today"),
  });
}
