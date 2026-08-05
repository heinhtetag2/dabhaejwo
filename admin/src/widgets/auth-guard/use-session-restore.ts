"use client";

import { useEffect } from "react";

import type { Operator } from "@/shared/lib/auth-store";
import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/**
 * 새로고침 후 세션 되살리기.
 *
 * <p>액세스 토큰은 메모리에만 있어 새로고침하면 사라진다. 하지만 리프레시 토큰은
 * <b>httpOnly 쿠키</b>로 브라우저에 남아 있으므로, 그것으로 새 액세스 토큰을 받아 온다.
 * 쿠키는 자바스크립트가 읽지 못하고 브라우저가 알아서 실어 보낸다 —
 * 그래서 이 코드에는 토큰이 한 번도 등장하지 않는다.
 *
 * <p>두 번 부르는 이유가 있다. 재발급은 <b>토큰만</b> 주고 내가 누구인지는 알려주지 않는다.
 * 운영자 정보가 없으면 사이드바에 이름이 안 뜨고 역할을 몰라 권한 판정이 전부 거짓이 되어
 * <b>메뉴가 통째로 사라진다.</b> 그래서 토큰을 먼저 심고 {@code /api/ops/me} 를 잇는다.
 */
export function useSessionRestore() {
  const status = useAuthStore((state) => state.status);

  useEffect(() => {
    // 이미 판정이 끝났으면 아무것도 하지 않는다. 로그인 직후에도 여기로 들어오는데
    // 그때 재발급을 또 부르면 방금 받은 세션을 굳이 한 번 더 확인하는 셈이다.
    if (status !== "unknown") {
      return;
    }

    let cancelled = false;

    const restore = async () => {
      try {
        const { accessToken } = await api<{ accessToken: string }>("/api/auth/refresh", {
          method: "POST",
          // 어느 쿠키를 쓸지 고른다. 두 콘솔이 같은 API 를 보므로 주체별로 나눠 두었다.
          query: { scope: "OPS" },
        });
        if (cancelled) return;

        // /api/ops/me 가 이 토큰을 써야 하므로 먼저 심는다. 아직 status 는 unknown 이라
        // 화면은 계속 기다린다 — 반쪽짜리 세션으로 대시보드를 그리지 않는다.
        useAuthStore.getState().setAccessToken(accessToken);

        const operator = await api<Operator>("/api/ops/me");
        if (cancelled) return;

        useAuthStore.getState().signIn({ accessToken }, operator);
      } catch {
        // 쿠키가 없거나 만료됐다. 정상적인 경우이므로 조용히 로그인으로 보낸다.
        if (!cancelled) {
          useAuthStore.getState().markAnonymous();
        }
      }
    };

    void restore();
    return () => {
      cancelled = true;
    };
  }, [status]);

  return status;
}
