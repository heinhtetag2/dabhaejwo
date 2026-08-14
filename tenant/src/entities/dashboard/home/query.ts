"use client";

import { useQuery } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { homeSummarySchema } from "./schema";
import type { HomeSummary } from "./types";
import { botApi, botKey, useCurrentBotId } from "@/shared/lib/current-bot";

export const homeKeys = {
  summary: ["home", "detail", "summary"] as const,
};

export function useHomeSummaryQuery() {
  const botId = useCurrentBotId();
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<HomeSummary>({
    queryKey: botKey(botId, homeKeys.summary),
    enabled: botId !== null && (accessToken !== null),
    queryFn: async () => homeSummarySchema.parse(await api(botApi(botId, "/home"))),
  });
}
