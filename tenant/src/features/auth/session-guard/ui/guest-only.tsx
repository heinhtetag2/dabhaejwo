"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { ROUTES } from "@/shared/config/routes";

import { useSessionRestore } from "../model/use-session-restore";

/**
 * 이미 로그인한 사람은 대시보드로 보낸다. 로그인·가입 화면에 건다.
 *
 * <p>로그인한 채로 가입 화면을 보면 "계정을 또 만들어야 하나" 하고 헷갈린다.
 *
 * <p>여기서도 세션 복원을 돌린다 — 리프레시 쿠키가 살아 있는 사람이 주소창으로 로그인
 * 화면에 들어오면 <b>다시 로그인시키지 않고</b> 대시보드로 보내야 한다.
 * 랜딩·요금제 같은 공개 화면에는 걸지 않으므로 방문자에게 불필요한 요청이 가지 않는다.
 */
export function GuestOnly({ children }: { children: ReactNode }) {
  const router = useRouter();
  const status = useSessionRestore();

  useEffect(() => {
    if (status === "authenticated") {
      router.replace(ROUTES.home);
    }
  }, [status, router]);

  if (status !== "anonymous") {
    // 판정 중이거나(unknown) 이미 로그인이라 이동 중이다. 로그인 폼을 잠깐 보여줬다가
    // 곧바로 치우면 깜빡임으로 보인다.
    return null;
  }
  return <>{children}</>;
}
