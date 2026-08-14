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

/**
 * 서비스 삭제 예약.
 *
 * 위젯은 즉시 멈추고 데이터는 유예 기간 뒤에 지워진다 — 그때까지는 되돌릴 수 있다.
 */
export function useDeleteBot() {
  const queryClient = useQueryClient();
  return useMutation<void, unknown, string>({
    mutationFn: (id) => api(`/api/app/bots/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: botKeys.list });
      void queryClient.invalidateQueries({ queryKey: sessionKeys.context });
    },
  });
}

export function useRestoreBot() {
  const queryClient = useQueryClient();
  return useMutation<Bot, unknown, string>({
    mutationFn: async (id) =>
      botSchema.parse(await api(`/api/app/bots/${id}/restore`, { method: "POST" })),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: botKeys.list });
      void queryClient.invalidateQueries({ queryKey: sessionKeys.context });
    },
  });
}
