"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { botSchema, sessionKeys, type Bot } from "@/entities/auth/session";
import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/**
 * 서비스 — 챗봇 한 벌. 화면 용어는 "서비스", API 는 `bot` 이다
 * (`docs/plan/service-plan.md` §3).
 */
export const botKeys = {
  list: ["bots", "list"] as const,
};

const botListSchema = z.array(botSchema);

export function useBotListQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);
  return useQuery<Bot[]>({
    queryKey: botKeys.list,
    enabled: accessToken !== null,
    queryFn: async () => botListSchema.parse(await api("/api/app/bots")),
  });
}

export interface BotSaveInput {
  name: string;
  primaryDomain: string;
}

export function useCreateBot() {
  const queryClient = useQueryClient();
  return useMutation<Bot, unknown, BotSaveInput>({
    mutationFn: async (input) =>
      botSchema.parse(await api("/api/app/bots", { method: "POST", body: input })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: botKeys.list });
      // 선택기·설치 화면이 컨텍스트의 bots[] 를 보므로 함께 새로 읽는다.
      void queryClient.invalidateQueries({ queryKey: sessionKeys.context });
    },
  });
}

export function useUpdateBot() {
  const queryClient = useQueryClient();
  return useMutation<Bot, unknown, BotSaveInput & { id: string }>({
    mutationFn: async ({ id, ...input }) =>
      botSchema.parse(await api(`/api/app/bots/${id}`, { method: "PATCH", body: input })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: botKeys.list });
      void queryClient.invalidateQueries({ queryKey: sessionKeys.context });
    },
  });
}
