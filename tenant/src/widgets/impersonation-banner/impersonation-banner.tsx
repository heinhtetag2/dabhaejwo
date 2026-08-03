"use client";

import dayjs from "dayjs";

import { useAuthStore } from "@/shared/lib/auth-store";

/**
 * 운영팀 대리 접속 배너.
 *
 * 세션 중에는 화면 상단에 고정 노출한다. 운영자가 자기 계정으로 착각한 채
 * 데이터를 변경하는 사고를 막기 위함이다 (tenant-plan.md §6.2).
 *
 * 프로토타입에는 없던 요소다 — 기획서에만 있고 구현이 빠져 있었다.
 */
export function ImpersonationBanner() {
  const impersonation = useAuthStore((state) => state.impersonation);

  if (!impersonation) {
    return null;
  }

  return (
    <div
      role="status"
      className="sticky top-0 z-60 flex flex-wrap items-center justify-center gap-x-3 gap-y-1 bg-brick px-4 py-2 text-center text-[13px] text-white"
    >
      <strong className="font-semibold">운영팀이 대리 접속 중입니다</strong>
      <span className="text-white/85">
        {impersonation.operatorName} · {impersonation.reason}
      </span>
      <span className="tabular text-[11.5px] text-white/70">
        {dayjs(impersonation.expiresAt).format("HH:mm")} 만료
      </span>
    </div>
  );
}
