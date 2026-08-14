"use client";

import { useQuery } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { faqListSchema } from "./schema";
import type { Faq } from "./types";
import { botApi, botKey, useCurrentBotId } from "@/shared/lib/current-bot";

export const faqKeys = {
  list: ["faq", "list"] as const,
};

export function useFaqListQuery() {
  const botId = useCurrentBotId();
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<Faq[]>({
    queryKey: botKey(botId, faqKeys.list),
    enabled: botId !== null && (accessToken !== null),
    queryFn: async () => faqListSchema.parse(await api(botApi(botId, "/faqs"))),
  });
}
