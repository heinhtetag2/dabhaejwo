"use client";

import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { ApiError, api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/** 챗봇 설정. 키는 api-contracts.md §9-4 와 동일하다. */
export type LauncherSize = "SMALL" | "MEDIUM" | "LARGE";

/** 화면에 띄울 설명. 실제 픽셀은 서버·위젯이 정한다 — 여기 값은 안내용이다. */
export const LAUNCHER_SIZE_LABEL: Record<LauncherSize, string> = {
  SMALL: "작게 (48px)",
  MEDIUM: "보통 (56px)",
  LARGE: "크게 (64px)",
};

/**
 * 올린 이미지 뒤에 깔 것.
 *
 * PNG 의 투명은 흰색이 아니라 **아무것도 안 칠한 것**이라 뒤에 있는 게 그대로 올라온다.
 * 브랜드 색 하나로 두면 흰 바탕 기준 로고는 진한 색 위에서 뭉개지고, 밝은 브랜드 색에
 * 흰 로고를 올리면 아예 안 보인다.
 */
export type LauncherBackground = "BRAND" | "WHITE" | "NONE";

export const LAUNCHER_BACKGROUND_LABEL: Record<LauncherBackground, string> = {
  BRAND: "브랜드 색",
  WHITE: "흰색",
  NONE: "없음 (투명)",
};

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
  /** 끄면 방문자에게 위젯이 아예 뜨지 않는다. 오류를 그리지도 않는다. */
  widgetEnabled: boolean;
  launcherSize: LauncherSize;
  /** 이미지 뒤에 깔 것. 이미지가 없으면 위젯에는 늘 `BRAND` 로 나간다. */
  launcherBackground: LauncherBackground;
  /** 업로드한 런처 아이콘. 없으면 로고, 그것도 없으면 기본 말풍선. */
  launcherIconUrl: string | null;
  /** 업체 로고. 콘솔 사이드바와 (아이콘이 없으면) 런처에 쓰인다. */
  logoUrl: string | null;
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
  widgetEnabled: z.boolean(),
  launcherSize: z.enum(["SMALL", "MEDIUM", "LARGE"]),
  launcherBackground: z.enum(["BRAND", "WHITE", "NONE"]),
  launcherIconUrl: z.string().nullable(),
  logoUrl: z.string().nullable(),
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
/**
 * 편집 중인 설정 한 벌.
 *
 * <p>말투 화면과 위젯 관리 화면이 **같은 리소스**를 나눠 편집한다. 각자 상태를 들면 한쪽에서
 * 저장할 때 다른 쪽이 들고 있던 옛 값까지 함께 덮어써 서로의 변경을 지운다 —
 * 저장 본문이 설정 전체이기 때문이다. 그래서 초안 관리를 여기 한 곳에 둔다.
 *
 * <p>손대기 전에는 서버 값을 그대로 쓰고, 손댄 뒤에만 로컬 값이 이긴다. 서버 값을 상태로
 * 복사해 두면 다시 불러왔을 때 어긋난다.
 */
export function useBotSettingsDraft() {
  const query = useBotSettingsQuery();
  const save = useSaveBotSettings();
  const [localDraft, setLocalDraft] = useState<BotSettings | null>(null);

  const draft = localDraft ?? query.data;

  return {
    query,
    save,
    draft,
    dirty: localDraft !== null,
    patch: (changes: Partial<BotSettings>) => {
      if (draft) setLocalDraft({ ...draft, ...changes });
    },
    reset: () => setLocalDraft(null),
    submit: (onError: (message: string) => void) => {
      if (!draft) return;
      save.mutate(draft, {
        // 저장이 끝나면 서버 값이 진실이다. 로컬 편집본을 버려 다음 조회 결과를 그대로 쓴다.
        onSuccess: () => setLocalDraft(null),
        onError: (cause: unknown) =>
          onError(cause instanceof ApiError ? cause.message : "저장하지 못했습니다"),
      });
    },
  };
}

/** 로고·런처 아이콘 업로드. 형식·크기 검증은 서버가 한다 — 여기서 막는 것은 UX 다. */
export function useUploadBrandingImage(kind: "logo" | "launcher-icon") {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData();
      form.append("file", file);
      return api<{ url: string }>(`/api/app/appearance/${kind}`, { method: "POST", body: form });
    },
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: botSettingsKeys.current }),
  });
}

export function useRemoveBrandingImage(kind: "logo" | "launcher-icon") {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api<void>(`/api/app/appearance/${kind}`, { method: "DELETE" }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: botSettingsKeys.current }),
  });
}

export function useSaveBotSettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (settings: BotSettings) =>
      api("/api/app/appearance", { method: "PUT", body: settings }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: botSettingsKeys.current }),
  });
}
