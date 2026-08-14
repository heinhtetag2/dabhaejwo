"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";

import type { Bot } from "@/entities/auth/session";
import { ROUTES, botRoute } from "@/shared/config/routes";
import { cn } from "@/shared/lib/cn";

/**
 * 지금 보고 있는 서비스와, 다른 서비스로 가는 길.
 *
 * <p><b>서비스가 하나면 아무것도 달라지지 않는다.</b> 버튼도, 드롭다운 표식도,
 * `aria-haspopup` 도 붙이지 않는다 — 스크린 리더에 "메뉴 버튼"이라고 읽히는 것부터가
 * 대다수 업체에게는 없던 복잡함이다.
 */
export function BotSelector({
  bots,
  currentBotId,
  tenantName,
}: {
  bots: Bot[];
  currentBotId: string | null;
  tenantName?: string;
}) {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const current = bots.find((bot) => bot.id === currentBotId) ?? bots[0];

  if (bots.length <= 1) {
    return (
      <span className="min-w-0">
        {current?.name ?? tenantName ?? "—"}
        <span className="block truncate font-mono text-[10.5px] tracking-[0.06em] text-[#7e8f9c]">
          {current?.primaryDomain ?? ""}
        </span>
      </span>
    );
  }

  /*
   * 지금 화면과 같은 자리로 옮긴다 — 대화 로그를 보다 서비스를 바꾸면 그쪽 대화 로그다.
   * 다만 상세 id 와 검색·필터 쿼리는 버린다. A 에서 "환불"로 검색하던 상태를 B 로 옮기면
   * 0건이 뜨는데, 사용자는 그걸 "B 에는 대화가 없다"로 읽는다.
   */
  const screen = pathname.replace(/^\/app\/s\/[^/]+\/?/, "").split("/")[0] ?? "";

  return (
    <span className="min-w-0 flex-1">
      <span className="block truncate text-[10.5px] text-[#7e8f9c]">{tenantName}</span>
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
        className="flex w-full items-center gap-1.5 text-left text-[13px] text-[#dde6eb]"
      >
        <span className="min-w-0 flex-1 truncate">{current?.name ?? "—"}</span>
        <span aria-hidden className="shrink-0 text-[10px] text-[#7e8f9c]">
          {open ? "▲" : "▼"}
        </span>
      </button>
      <span className="block truncate font-mono text-[10.5px] tracking-[0.06em] text-[#7e8f9c]">
        {current?.primaryDomain ?? ""}
      </span>

      {open ? (
        <span role="menu" className="mt-2 block rounded-lg bg-white/8 p-1">
          {bots.map((bot) => (
            <Link
              key={bot.id}
              role="menuitem"
              href={botRoute(bot.id, screen as never)}
              onClick={() => setOpen(false)}
              className={cn(
                "block rounded-md px-2 py-1.5 text-[12.5px]",
                bot.id === current?.id ? "bg-white/10 text-white" : "text-[#c9d4dc] hover:bg-white/8",
              )}
            >
              <span className="block truncate">{bot.name}</span>
              {/* 색만으로 구분하지 않는다 — 작동 여부는 글자로도 말한다 (WCAG 2.1 AA) */}
              <span className="block truncate text-[10.5px] text-[#7e8f9c]">
                {bot.lastCalledAt ? bot.primaryDomain : `${bot.primaryDomain} · 설치 확인 안 됨`}
              </span>
            </Link>
          ))}
          <Link
            href={ROUTES.bots}
            onClick={() => setOpen(false)}
            className="mt-1 block rounded-md px-2 py-1.5 text-[12px] text-[#9fb0bc] hover:bg-white/8"
          >
            서비스 관리
          </Link>
        </span>
      ) : null}
    </span>
  );
}
