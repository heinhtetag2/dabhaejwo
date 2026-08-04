"use client";

import { useQuery } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { homeSummarySchema } from "./schema";
import type { HomeSummary } from "./types";

export const homeKeys = {
  summary: ["home", "detail", "summary"] as const,
};

export function useHomeSummaryQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<HomeSummary>({
    queryKey: homeKeys.summary,
    enabled: accessToken !== null,
    queryFn: async () => homeSummarySchema.parse(await api("/api/app/home")),
  });
}
