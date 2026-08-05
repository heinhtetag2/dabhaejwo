"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { ROUTES } from "@/shared/config/routes";

import { useSessionRestore } from "./use-session-restore";

/**
 * 인증 가드.
 *
 * **이건 UX 이지 보안이 아니다.** 권한의 진실은 서버이며 각 API 가
 * `@RequirePermission` 으로 다시 검증한다. 여기서 하는 일은 토큰이 없는 사람에게
 * 빈 화면 대신 로그인을 보여주는 것뿐이다.
 *
 * 상태가 셋인 이유 — 새로고침 직후에는 메모리가 비어 있어도 리프레시 쿠키로
 * **세션이 되살아날 수 있다.** "토큰 없음 = 로그아웃"으로 단정하면 복원해 보기도 전에
 * 로그인으로 튕긴다(실제로 그랬다). 판정이 끝날 때까지 기다린다.
 */
export function AuthGuard({ children }: { children: ReactNode }) {
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
    // 리다이렉트가 끝나기 전 한 프레임 동안 보호 화면이 그려지지 않게 막는다.
    return null;
  }

  return <>{children}</>;
}
