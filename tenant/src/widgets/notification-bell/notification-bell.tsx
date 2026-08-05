"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import {
  type AppNotification,
  type NotificationSeverity,
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotificationListQuery,
  useNotificationSocket,
  useUnreadCountQuery,
} from "@/entities/dashboard/notification";
import { ApiError } from "@/shared/api/http-client";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { cn } from "@/shared/lib/cn";
import { relative } from "@/shared/lib/format";

// admin/src/widgets/notification-bell/notification-bell.tsx 와 짝이다. 강조색과 종류가
// 다를 뿐 동작은 같다 — 한쪽 동작을 고치면 같은 커밋에서 다른 쪽도 맞춘다.

/** 중요도는 색만으로 구분하지 않는다 — 라벨을 함께 낸다 (WCAG 2.1 AA). */
const SEVERITY: Record<NotificationSeverity, { label: string; dot: string }> = {
  HIGH: { label: "중요", dot: "bg-brick" },
  NORMAL: { label: "알림", dot: "bg-mark" },
  LOW: { label: "참고", dot: "bg-slate-2" },
};

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const router = useRouter();
  const containerRef = useRef<HTMLDivElement>(null);

  const unread = useUnreadCountQuery();
  const list = useNotificationListQuery(open);
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();

  useNotificationSocket();

  useEffect(() => {
    if (!open) return;

    const onPointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  const count = unread.data?.count ?? 0;

  const openTarget = (notification: AppNotification) => {
    if (!notification.read) {
      // 대리 접속 중에는 서버가 거절한다. 그래도 이동은 막지 않는다 —
      // 운영자가 화면을 보는 것 자체는 정상 동작이다.
      markRead.mutate(notification.id);
    }
    if (notification.targetPath) {
      setOpen(false);
      router.push(notification.targetPath);
    }
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((was) => !was)}
        aria-expanded={open}
        aria-label={count > 0 ? `알림 ${count}건` : "알림"}
        className={cn(
          "relative grid size-9 place-items-center rounded-full border border-line bg-card",
          "text-slate transition-colors hover:bg-line-2 hover:text-ink",
        )}
      >
        <BellIcon />
        {count > 0 ? (
          <span
            className={cn(
              "absolute -top-1 -right-1 grid min-w-[18px] place-items-center rounded-full",
              "bg-brick px-1 font-mono text-[10px] leading-[17px] font-semibold text-white",
            )}
          >
            {count > 99 ? "99+" : count}
          </span>
        ) : null}
      </button>

      {open ? (
        <div
          role="dialog"
          aria-label="알림"
          className={cn(
            "absolute right-0 z-70 mt-2 w-[360px] overflow-hidden rounded-card",
            "border border-line bg-card shadow-[0_12px_32px_rgba(23,34,46,0.14)]",
          )}
        >
          <header className="flex items-center gap-3 border-b border-line px-4 py-2.5">
            <b className="text-[13.5px] font-semibold">알림</b>
            {count > 0 ? (
              <span className="font-mono text-[11px] text-slate-2">안 읽음 {count}</span>
            ) : null}
            <button
              type="button"
              onClick={() => markAllRead.mutate()}
              disabled={count === 0 || markAllRead.isPending}
              className="ml-auto text-[12px] text-slate hover:text-ink disabled:text-slate-2/60"
            >
              모두 읽음
            </button>
          </header>

          <div className="max-h-[420px] overflow-y-auto">
            {list.isPending ? <LoadingState label="알림을 불러오는 중" /> : null}
            {list.isError ? (
              <ErrorState
                message={
                  list.error instanceof ApiError
                    ? list.error.message
                    : "알림을 불러오지 못했습니다"
                }
                onRetry={() => void list.refetch()}
              />
            ) : null}
            {list.data?.content.length === 0 ? (
              <EmptyState message="아직 알림이 없습니다" />
            ) : null}

            {list.data?.content.map((notification) => {
              const severity = SEVERITY[notification.severity] ?? SEVERITY.NORMAL;
              return (
                <button
                  key={notification.id}
                  type="button"
                  onClick={() => openTarget(notification)}
                  className={cn(
                    "flex w-full gap-2.5 border-b border-line-2 px-4 py-3 text-left last:border-b-0",
                    "transition-colors hover:bg-paper",
                    notification.read ? "bg-card" : "bg-mark-soft/40",
                  )}
                >
                  <i
                    aria-hidden
                    className={cn("mt-[7px] size-[6px] shrink-0 rounded-full", severity.dot)}
                  />
                  <span className="min-w-0 flex-1">
                    <span className="flex items-baseline gap-2">
                      <b className="truncate text-[13px] font-medium">{notification.title}</b>
                      <span className="ml-auto shrink-0 font-mono text-[10.5px] text-slate-2">
                        {relative(notification.createdAt)}
                      </span>
                    </span>
                    {notification.body ? (
                      <span className="mt-0.5 block text-[12px] leading-[1.5] text-slate">
                        {notification.body}
                      </span>
                    ) : null}
                    <span className="sr-only">{severity.label}</span>
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function BellIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M13.7 21a2 2 0 0 1-3.4 0"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
      />
    </svg>
  );
}
