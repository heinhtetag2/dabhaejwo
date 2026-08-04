"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/** 챗봇 설정. 키는 api-contracts.md §9-4 와 동일하다. */
export type WidgetPosition = "BOTTOM_RIGHT" | "BOTTOM_LEFT";
export type PageScope = "ALL" | "INCLUDE" | "EXCLUDE";

export interface BotSettings {
  botName: string;
  /** #RRGGBB. 위젯이 그대로 스타일에 넣으므로 서버가 형식을 검증한다. */
  brandColor: string;
  greeting: string;
  persona: string;
  fallbackMessage: string;
  forbiddenTopics: string[];
  leadCaptureEnabled: boolean;
  supportPhone: string | null;
  agentHandoffEnabled: boolean;
  agentHours: string | null;
  widgetPosition: WidgetPosition;
  pageScope: PageScope;
  pagePatterns: string[];
  /** 0 이면 자동으로 말 걸지 않는다. */
  nudgeDelaySeconds: number;
}

const botSettingsSchema = z.object({
  botName: z.string(),
  brandColor: z.string(),
  greeting: z.string(),
  persona: z.string(),
  fallbackMessage: z.string(),
  forbiddenTopics: z.array(z.string()),
  leadCaptureEnabled: z.boolean(),
  supportPhone: z.string().nullable(),
  agentHandoffEnabled: z.boolean(),
  agentHours: z.string().nullable(),
  widgetPosition: z.enum(["BOTTOM_RIGHT", "BOTTOM_LEFT"]),
  pageScope: z.enum(["ALL", "INCLUDE", "EXCLUDE"]),
  pagePatterns: z.array(z.string()),
  nudgeDelaySeconds: z.number(),
});

export const botSettingsKeys = {
  current: ["bot-settings", "detail", "current"] as const,
};

export function useBotSettingsQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<BotSettings>({
    queryKey: botSettingsKeys.current,
    enabled: accessToken !== null,
    queryFn: async () => botSettingsSchema.parse(await api("/api/app/appearance")),
  });
}

/** 전체를 한 번에 보낸다(PUT). 부분 갱신이면 "어느 필드를 보냈는가"를 양쪽이 계속 맞춰야 한다. */
export function useSaveBotSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (settings: BotSettings) =>
      api("/api/app/appearance", { method: "PUT", body: settings }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: botSettingsKeys.current }),
  });
}
