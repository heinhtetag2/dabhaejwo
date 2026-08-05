"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api, type PageResponse } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import type { AppNotification } from "./types";

const notificationSchema = z.object({
  id: z.number(),
  type: z.enum([
    "IMPERSONATION_STARTED",
    "QUOTA_WARNING",
    "QUOTA_EXHAUSTED",
    "TRIAL_ENDING",
    "INDEXING_DONE",
    "INDEXING_FAILED",
    "LEAD_RECEIVED",
    "ANSWER_GAPS_PILING",
  ]),
  severity: z.enum(["LOW", "NORMAL", "HIGH"]),
  title: z.string(),
  body: z.string().nullable(),
  targetPath: z.string().nullable(),
  read: z.boolean(),
  createdAt: z.string(),
});

const notificationPageSchema = z.object({
  content: z.array(notificationSchema),
  page: z.object({
    number: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  }),
});

export const notificationKeys = {
  all: ["notification"] as const,
  list: () => ["notification", "list"] as const,
  unreadCount: () => ["notification", "unread-count"] as const,
};

/** 목록은 벨을 열었을 때만 읽는다. 배지만 보려고 30건을 끌어올 이유가 없다. */
export function useNotificationListQuery(enabled = true) {
  const accessToken = useAuthStore((state) => state.accessToken);
  return useQuery<PageResponse<AppNotification>>({
    queryKey: notificationKeys.list(),
    enabled: enabled && accessToken !== null,
    queryFn: async () =>
      notificationPageSchema.parse(
        await api("/api/app/notifications", { query: { size: 30 } }),
      ),
  });
}

/**
 * 벨 배지.
 *
 * 소켓이 붙어 있으면 새 알림마다 무효화되므로 폴링은 <b>보험</b>이다 —
 * 소켓이 끊긴 채 화면을 열어둔 담당자에게도 결국 배지가 갱신된다.
 */
export function useUnreadCountQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);
  return useQuery({
    queryKey: notificationKeys.unreadCount(),
    enabled: accessToken !== null,
    queryFn: () => api<{ count: number }>("/api/app/notifications/unread-count"),
    refetchInterval: 60_000,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: number) =>
      api<void>(`/api/app/notifications/${notificationId}/read`, { method: "PATCH" }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      api<{ updated: number }>("/api/app/notifications/read-all", { method: "PATCH" }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}
