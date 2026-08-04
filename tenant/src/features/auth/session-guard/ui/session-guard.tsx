"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { useAuthStore } from "@/shared/lib/auth-store";
import { ROUTES } from "@/shared/config/routes";

/**
 * 보호 라우트 가드.
 *
 * <p>토큰이 메모리에만 있으므로 미들웨어(서버)에서는 로그인 여부를 알 수 없다.
 * 그래서 클라이언트에서 막는다. <b>이건 UX 이지 보안이 아니다</b> — 실제 차단은
 * 서버가 모든 {@code /api/app/**} 요청에서 한다.
 */
export function SessionGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  const accessToken = useAuthStore((state) => state.accessToken);

  useEffect(() => {
    if (accessToken === null) {
      router.replace(ROUTES.login);
    }
  }, [accessToken, router]);

  if (accessToken === null) {
    return (
      <div className="flex min-h-dvh items-center justify-center text-[13px] text-slate-2">
        로그인 화면으로 이동합니다…
      </div>
    );
  }

  return <>{children}</>;
}
