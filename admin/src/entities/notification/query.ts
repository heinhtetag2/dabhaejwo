import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import type { AppNotification } from "./types";

export const notificationKeys = {
  all: ["notification"] as const,
  list: () => [...notificationKeys.all, "list"] as const,
  unreadCount: () => [...notificationKeys.all, "unread-count"] as const,
};

export function useNotificationListQuery(enabled = true) {
  return useQuery({
    queryKey: notificationKeys.list(),
    queryFn: () => api<PageResponse<AppNotification>>("/api/ops/notifications", {
      query: { size: 30 },
    }),
    enabled,
  });
}

/**
 * 벨 배지.
 *
 * 소켓이 붙어 있으면 새 알림이 올 때마다 무효화되므로 폴링은 <b>보험</b>이다 —
 * 소켓이 끊긴 채로 화면을 열어둔 운영자에게도 결국 배지가 갱신된다.
 */
export function useUnreadCountQuery() {
  const signedIn = useAuthStore((state) => state.accessToken !== null);
  return useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: () => api<{ count: number }>("/api/ops/notifications/unread-count"),
    enabled: signedIn,
    refetchInterval: 60_000,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (notificationId: number) =>
      api<void>(`/api/ops/notifications/${notificationId}/read`, { method: "PATCH" }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      api<{ updated: number }>("/api/ops/notifications/read-all", { method: "PATCH" }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}
