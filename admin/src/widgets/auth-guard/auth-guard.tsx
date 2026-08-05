"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { ROUTES } from "@/shared/config/routes";
import { useAuthStore } from "@/shared/lib/auth-store";

/**
 * 인증 가드.
 *
 * **이건 UX 이지 보안이 아니다.** 권한의 진실은 서버이며 각 API 가
 * `@RequirePermission` 으로 다시 검증한다. 여기서 하는 일은 토큰이 없는 사람에게
 * 빈 화면 대신 로그인을 보여주는 것뿐이다.
 *
 * 토큰을 메모리에만 두므로 새로고침하면 로그인으로 돌아온다 — 지속 세션은
 * 서버가 리프레시 토큰을 httpOnly 쿠키로 내려줘야 안전해진다 (docs/IMPROVEMENTS.md).
 */
export function AuthGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  const accessToken = useAuthStore((state) => state.accessToken);

  useEffect(() => {
    if (!accessToken) {
      router.replace(ROUTES.login);
    }
  }, [accessToken, router]);

  if (!accessToken) {
    // 리다이렉트가 끝나기 전 한 프레임 동안 보호 화면이 그려지지 않게 막는다.
    return null;
  }

  return <>{children}</>;
}
