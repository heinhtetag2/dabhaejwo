"use client";

import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

import { ROUTES } from "@/shared/config/routes";
import { useAuthStore } from "@/shared/lib/auth-store";

/**
 * 이미 로그인한 사람은 대시보드로 보낸다. 로그인·가입 화면에 건다.
 *
 * <p>로그인한 채로 가입 화면을 보면 "계정을 또 만들어야 하나" 하고 헷갈린다.
 */
export function GuestOnly({ children }: { children: ReactNode }) {
  const router = useRouter();
  const accessToken = useAuthStore((state) => state.accessToken);

  useEffect(() => {
    if (accessToken !== null) {
      router.replace(ROUTES.home);
    }
  }, [accessToken, router]);

  if (accessToken !== null) {
    return null;
  }
  return <>{children}</>;
}
