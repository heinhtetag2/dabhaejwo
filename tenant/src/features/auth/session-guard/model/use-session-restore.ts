"use client";

import { useEffect } from "react";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/**
 * 새로고침 후 세션 되살리기.
 *
 * <p>액세스 토큰은 메모리에만 있어 새로고침하면 사라진다. 하지만 리프레시 토큰은
 * <b>httpOnly 쿠키</b>로 브라우저에 남아 있으므로 그것으로 새 액세스 토큰을 받아 온다.
 * 쿠키는 자바스크립트가 읽지 못하고 브라우저가 알아서 실어 보낸다 —
 * 그래서 이 코드에는 토큰이 한 번도 등장하지 않는다.
 *
 * <p>운영 콘솔과 달리 담당자 정보를 여기서 받지 않는다. 업체 대시보드는 그것을 서버 상태로
 * 다루고({@code entities/auth/session} 의 {@code /api/app/me}) 화면이 필요할 때 읽는다 —
 * 같은 데이터를 스토어에도 두면 반드시 어긋난다.
 */
export function useSessionRestore() {
  const status = useAuthStore((state) => state.status);

  useEffect(() => {
    // 이미 판정이 끝났으면 아무것도 하지 않는다.
    if (status !== "unknown") {
      return;
    }

    let cancelled = false;

    void (async () => {
      try {
        const { accessToken } = await api<{ accessToken: string }>("/api/auth/refresh", {
          method: "POST",
          // 어느 쿠키를 쓸지 고른다. 두 콘솔이 같은 API 를 보므로 주체별로 나눠 두었다.
          query: { scope: "APP" },
        });
        if (!cancelled) {
          useAuthStore.getState().setAccessToken(accessToken);
        }
      } catch {
        // 쿠키가 없거나 만료됐다. 정상적인 경우이므로 조용히 로그인으로 보낸다.
        if (!cancelled) {
          useAuthStore.getState().markAnonymous();
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [status]);

  return status;
}
