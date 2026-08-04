"use client";

import { useQuery } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

import { appContextSchema } from "./schema";
import type { AppContext } from "./types";

export const sessionKeys = {
  context: ["session", "detail", "me"] as const,
};

/**
 * 대시보드 부팅 컨텍스트. 셸이 한 번 부르고 사이드바·배너·사용량 미터가 함께 쓴다.
 * 토큰이 없으면 호출하지 않는다 — 401 을 만들어 로그아웃 처리를 유발할 이유가 없다.
 */
export function useAppContextQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<AppContext>({
    queryKey: sessionKeys.context,
    enabled: accessToken !== null,
    queryFn: async () => appContextSchema.parse(await api("/api/app/me")),
  });
}
