"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";

import { env } from "@/shared/config/env";
import { useAuthStore } from "@/shared/lib/auth-store";

import { notificationKeys } from "./query";
import type { AppNotification } from "./types";

/** 끊겼을 때 다시 붙기까지. 늘려가며 시도해 서버가 죽어 있을 때 두드리지 않는다. */
const RETRY_MS = [1_000, 3_000, 10_000, 30_000] as const;

function socketUrl(): string {
  const base = new URL("/ws/notifications", env.apiBaseUrl);
  base.protocol = base.protocol === "https:" ? "wss:" : "ws:";
  return base.toString();
}

/**
 * 알림 소켓.
 *
 * <p>토큰은 <b>첫 프레임</b>으로 보낸다. 쿼리 파라미터로 보내면 액세스 토큰이
 * 접근 로그·프록시 로그에 남는다 (api `NotificationWebSocketHandler` 와 같은 약속).
 *
 * <p>소켓이 없어도 화면은 동작한다 — 목록은 REST 로 읽고 배지는 폴링이 갱신한다.
 * 소켓은 <b>빠르게 아는 수단</b>이지 알림의 저장소가 아니다.
 */
export function useNotificationSocket(onArrive?: (notification: AppNotification) => void) {
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((state) => state.accessToken);

  // 콜백이 매 렌더 새로 만들어져도 소켓을 다시 열지 않게 한다.
  // 갱신은 렌더가 아니라 effect 에서 한다 — 렌더 중 ref 를 쓰면 동시성 모드에서
  // 버려지는 렌더의 값이 남을 수 있다.
  const arriveRef = useRef(onArrive);
  useEffect(() => {
    arriveRef.current = onArrive;
  }, [onArrive]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    let socket: WebSocket | null = null;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;
    let attempt = 0;
    let closed = false;

    const connect = () => {
      if (closed) return;

      try {
        socket = new WebSocket(socketUrl());
      } catch {
        // URL 이 잘못됐거나 브라우저가 막았다. 재시도해도 같으므로 조용히 포기한다.
        return;
      }

      socket.onopen = () => {
        attempt = 0;
        socket?.send(JSON.stringify({ type: "AUTH", token: accessToken }));
      };

      socket.onmessage = (event) => {
        let frame: { type?: string; notification?: AppNotification };
        try {
          frame = JSON.parse(String(event.data)) as typeof frame;
        } catch {
          return;
        }
        if (frame.type !== "NOTIFICATION" || !frame.notification) {
          return;
        }
        // 목록과 배지는 서버가 진실이다. 소켓은 "지금 다시 읽어라"는 신호로만 쓴다 —
        // 프레임으로 캐시를 직접 조작하면 소켓이 한 번 끊긴 사이 목록이 어긋난다.
        void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
        arriveRef.current?.(frame.notification);
      };

      socket.onclose = () => {
        if (closed) return;
        const delay = RETRY_MS[Math.min(attempt, RETRY_MS.length - 1)];
        attempt += 1;
        retryTimer = setTimeout(connect, delay);
      };
    };

    connect();

    return () => {
      closed = true;
      if (retryTimer) clearTimeout(retryTimer);
      socket?.close();
    };
  }, [accessToken, queryClient]);
}
