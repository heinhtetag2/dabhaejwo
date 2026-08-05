"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { ROUTES } from "@/shared/config/routes";

import { useSessionRestore } from "../model/use-session-restore";

/**
 * 보호 라우트 가드.
 *
 * <p>토큰이 메모리에만 있으므로 미들웨어(서버)에서는 로그인 여부를 알 수 없다.
 * 그래서 클라이언트에서 막는다. <b>이건 UX 이지 보안이 아니다</b> — 실제 차단은
 * 서버가 모든 {@code /api/app/**} 요청에서 한다.
 *
 * <p>상태가 셋인 이유 — 새로고침 직후에는 메모리가 비어 있어도 리프레시 쿠키로
 * <b>세션이 되살아날 수 있다.</b> "토큰 없음 = 로그아웃"으로 단정하면 복원해 보기도 전에
 * 로그인으로 튕긴다(실제로 그랬다). 판정이 끝날 때까지 기다린다.
 */
export function SessionGuard({ children }: { children: ReactNode }) {
  const router = useRouter();
  const status = useSessionRestore();

  useEffect(() => {
    if (status === "anonymous") {
      router.replace(ROUTES.login);
    }
  }, [status, router]);

  if (status === "unknown") {
    return (
      <div className="flex min-h-dvh items-center justify-center text-[13px] text-slate-2">
        세션을 확인하는 중…
      </div>
    );
  }

  if (status === "anonymous") {
    return (
      <div className="flex min-h-dvh items-center justify-center text-[13px] text-slate-2">
        로그인 화면으로 이동합니다…
      </div>
    );
  }

  return <>{children}</>;
}
