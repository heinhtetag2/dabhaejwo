"use client";

import { useQuery } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { faqListSchema } from "./schema";
import type { Faq } from "./types";

export const faqKeys = {
  list: ["faq", "list"] as const,
};

export function useFaqListQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<Faq[]>({
    queryKey: faqKeys.list,
    enabled: accessToken !== null,
    queryFn: async () => faqListSchema.parse(await api("/api/app/faqs")),
  });
}
