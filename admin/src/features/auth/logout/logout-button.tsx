"use client";

import { useState } from "react";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";
import { cn } from "@/shared/lib/cn";

/**
 * 로그아웃.
 *
 * <p><b>서버를 반드시 부른다.</b> 메모리만 비우면 리프레시 쿠키가 그대로 남아 새로고침
 * 한 번에 다시 로그인된 상태로 돌아온다 — 공용 PC 에서는 사고다. 쿠키는 자바스크립트가
 * 지울 수 없으므로(httpOnly) 서버가 지워 줘야 한다.
 *
 * <p>요청이 실패해도 화면은 로그아웃시킨다. 여기서 붙잡아 두면 사용자는 "로그아웃이
 * 안 된다"고 느끼고, 그 상태로 자리를 뜨는 편이 더 위험하다. 다만 쿠키가 남았을 수 있으니
 * 실패는 조용히 넘기지 않고 콘솔에 남긴다.
 */
export function LogoutButton({ className }: { className?: string }) {
  const [pending, setPending] = useState(false);

  const logout = async () => {
    setPending(true);
    try {
      // 자기 주체의 쿠키만 지운다 — 다른 콘솔에 로그인해 둔 세션까지 끊지 않는다.
      await api<void>("/api/auth/logout", { method: "POST", query: { scope: "OPS" } });
    } catch (cause) {
      console.warn("로그아웃 요청이 실패했습니다. 쿠키가 남아 있을 수 있습니다.", cause);
    } finally {
      useAuthStore.getState().signOut();
    }
  };

  return (
    <button
      type="button"
      onClick={() => void logout()}
      disabled={pending}
      className={cn(
        "rounded-[6px] px-2 py-1 text-[11.5px] transition-colors",
        "text-[#8fa3b0] hover:bg-white/8 hover:text-white disabled:opacity-50",
        className,
      )}
    >
      {pending ? "나가는 중…" : "로그아웃"}
    </button>
  );
}
