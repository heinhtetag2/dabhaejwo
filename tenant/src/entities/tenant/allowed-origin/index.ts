"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";
import { botApi, botKey, useCurrentBotId } from "@/shared/lib/current-bot";

/** 위젯이 동작해도 되는 주소. lastCalledAt 이 null 이면 아직 설치가 확인되지 않은 것이다. */
export interface AllowedOrigin {
  id: string;
  origin: string;
  lastCalledAt: string | null;
}

const allowedOriginSchema = z.object({
  id: z.string(),
  origin: z.string(),
  lastCalledAt: z.string().nullable(),
});

export const allowedOriginKeys = {
  list: ["allowed-origin", "list"] as const,
};

export function useAllowedOriginsQuery() {
  const botId = useCurrentBotId();
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<AllowedOrigin[]>({
    queryKey: botKey(botId, allowedOriginKeys.list),
    enabled: botId !== null && (accessToken !== null),
    queryFn: async () =>
      z.array(allowedOriginSchema).parse(await api(botApi(botId, "/allowed-origins"))),
  });
}

function useInvalidateOrigins() {
  const queryClient = useQueryClient();
  return () => void queryClient.invalidateQueries({ queryKey: allowedOriginKeys.list });
}

export function useAddAllowedOrigin() {
  const botId = useCurrentBotId();
  const invalidate = useInvalidateOrigins();
  return useMutation({
    mutationFn: (origin: string) =>
      api(botApi(botId, "/allowed-origins"), { method: "POST", body: { origin } }),
    onSuccess: invalidate,
  });
}

export function useRemoveAllowedOrigin() {
  const botId = useCurrentBotId();
  const invalidate = useInvalidateOrigins();
  return useMutation({
    mutationFn: (id: string) => api(botApi(botId, `/allowed-origins/${id}`), { method: "DELETE" }),
    onSuccess: invalidate,
  });
}
