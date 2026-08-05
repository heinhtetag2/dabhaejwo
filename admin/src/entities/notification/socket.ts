"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";

import { env } from "@/shared/config/env";
import { useAuthStore } from "@/shared/lib/auth-store";

import { notificationKeys } from "./query";
import type { AppNotification } from "./types";

/** 끊겼을 때 다시 붙기까지. 늘려가며 시도해 서버가 죽어 있을 때 두드리지 않는다. */
const RETRY_MS = [1_000, 3_000, 10_000, 30_000] as const;

/** 서버가 첫 프레임 인증을 거절할 때 쓰는 종료 코드(CloseStatus.NOT_ACCEPTABLE). */
const AUTH_REJECTED = 1003;

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
        // **여기서 attempt 를 되돌리지 않는다.** 핸드셰이크는 토큰이 틀려도 성공한다
        // (인증은 그다음 프레임에서 한다). 열렸다고 성공으로 세면 백오프가 영원히
        // 1초에 머물러 서버를 2초마다 두드리게 된다 — 실제로 그렇게 났다.
        socket?.send(JSON.stringify({ type: "AUTH", token: accessToken }));
      };

      socket.onmessage = (event) => {
        let frame: { type?: string; notification?: AppNotification };
        try {
          frame = JSON.parse(String(event.data)) as typeof frame;
        } catch {
          return;
        }
        // 인증까지 통과한 이 시점이 진짜 성공이다. 다음 끊김은 처음부터 다시 센다.
        if (frame.type === "READY") {
          attempt = 0;
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

      socket.onclose = (event) => {
        if (closed) return;

        // 서버가 인증을 거절했다(1003). 같은 토큰으로 다시 붙어도 결과가 같으므로
        // **재시도하지 않는다.** 토큰이 새로 발급되면 effect 가 다시 돌며 연결한다.
        // 이 분기가 없으면 만료된 토큰을 든 탭이 서버를 무한히 두드린다.
        if (event.code === AUTH_REJECTED) {
          return;
        }

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
