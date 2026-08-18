"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { useAppContextQuery } from "@/entities/auth/session";
import { useBotSettingsQuery } from "@/entities/chatbot/bot-settings";
import { useAllowedOriginsQuery } from "@/entities/tenant/allowed-origin";
import { env } from "@/shared/config/env";
import { LogoutButton } from "@/features/auth/logout";
import { ACCOUNT_NAV, NAV_GROUPS, ROUTES, botRoute } from "@/shared/config/routes";
import { useNavBotId } from "@/shared/lib/current-bot";
import { BotSelector } from "@/features/bot/ui/bot-selector";
import { cn } from "@/shared/lib/cn";

export function TenantSidebar() {
  const pathname = usePathname();
  const { data } = useAppContextQuery();
  // 계정 화면에는 URL 에 서비스가 없다. 그래도 링크는 어딘가를 가리켜야 한다.
  const navBotId = useNavBotId(data?.bots);
  const { data: settings } = useBotSettingsQuery();
  const { data: origins } = useAllowedOriginsQuery();

  /**
   * 위젯이 실제로 도는지. 초록 불은 **확인된 사실**일 때만 켠다.
   *
   * `lastCalledAt` 은 그 주소에서 위젯이 서버를 부른 적이 있다는 뜻이다 —
   * 코드를 붙였는지 우리가 알 수 있는 유일한 신호다.
   */
  const called = (origins ?? []).some((origin) => origin.lastCalledAt !== null);
  const widgetState = !settings?.widgetEnabled
    ? { tone: "off" as const, label: "챗봇을 꺼둔 상태입니다" }
    : called
      ? { tone: "ok" as const, label: "사이트에서 작동 중" }
      : { tone: "off" as const, label: "아직 설치가 확인되지 않았습니다" };
  const usage = data?.usage;
  const usedPercent =
    usage && usage.convLimit > 0
      ? Math.min(100, Math.round((usage.convCount / usage.convLimit) * 100))
      : 0;

  return (
    <aside className="sticky top-0 flex h-dvh w-[236px] shrink-0 flex-col bg-ink text-[#d2d2d6]">
      <div className="border-b border-white/8 px-5 pt-5.5 pb-4.5">
        <div className="flex items-center gap-2.5">
          <span className="grid size-[22px] place-items-center rounded-md bg-mark font-mono text-xs font-bold text-white">
            A
          </span>
          <span className="font-semibold tracking-[-0.01em] text-white">답해줘</span>
        </div>
        {/*
          업체 로고. 없으면 자리를 비워 둔다 — 회색 상자를 그리면 "이미지가 깨졌나"로 읽힌다.
          위젯 관리 화면에서 올린다.
        */}
        <div className="mt-3.5 flex items-center gap-2.5 rounded-lg bg-white/6 px-2.5 py-2 text-[13px] text-[#e4e4e8]">
          {settings?.logoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={new URL(settings.logoUrl, env.apiBaseUrl).toString()}
              alt=""
              className="size-7 shrink-0 rounded-md bg-white/90 object-contain"
            />
          ) : null}
          <BotSelector bots={data?.bots ?? []} currentBotId={navBotId} tenantName={data?.tenant.name} />
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3.5">
        {NAV_GROUPS.map((group) => (
          <div key={group.label} className="mb-[18px]">
            <span className="block px-2 pb-[7px] font-mono text-[10px] tracking-[0.11em] text-[#7d7d86] uppercase">
              {group.label}
            </span>
            {group.items.map((item) => {
              const href = navBotId ? botRoute(navBotId, item.screen) : ROUTES.bots;
              // 홈은 접두사가 나머지 전부와 겹치므로 완전 일치로만 본다.
              const active = item.screen === "" ? pathname === href : pathname.startsWith(href);
              return (
                <Link
                  key={item.screen}
                  href={href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "mb-px flex w-full items-center gap-2.5 rounded-[7px] px-2.5 py-2 text-[13.5px] transition-colors",
                    active
                      ? "bg-mark/15 font-medium text-white"
                      : "text-[#c2c2c8] hover:bg-white/7 hover:text-white",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </div>
        ))}

        {/* 업체 단위 — 서비스를 바꿔도 그대로다. */}
        <div className="mb-[18px]">
          <span className="block px-2 pb-[7px] font-mono text-[10px] tracking-[0.11em] text-[#7d7d86] uppercase">
            계정
          </span>
          {ACCOUNT_NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              aria-current={pathname.startsWith(item.href) ? "page" : undefined}
              className={cn(
                "mb-px flex w-full items-center gap-2.5 rounded-[7px] px-2.5 py-2 text-[13.5px] transition-colors",
                pathname.startsWith(item.href)
                  ? "bg-mark/15 font-medium text-white"
                  : "text-[#c2c2c8] hover:bg-white/7 hover:text-white",
              )}
            >
              {item.label}
            </Link>
          ))}
        </div>
      </nav>

      <div className="border-t border-white/8 p-3.5">
        <div className="rounded-lg bg-white/5 px-3 py-[11px]">
          {/*
            전에는 무조건 "사이트에서 작동 중"이라고 찍었다. 아무것도 보지 않는 문구라,
            설치가 안 됐거나 챗봇을 꺼둔 업체에게도 초록 불이 켜져 있었다 —
            "작동 중이라는데 사이트엔 안 보인다"가 여기서 나온다.

            판단 근거는 둘이다: 업체가 켜뒀는가(`widgetEnabled`), 그리고 등록한 주소로
            실제 호출이 온 적이 있는가(`lastCalledAt`). 색만으로 구분하지 않고
            문구를 함께 바꾼다 (WCAG 2.1 AA).
          */}
          <div className="flex items-center gap-[7px] text-[12.5px] text-[#e4e4e8]">
            <span
              aria-hidden
              className={cn(
                "size-[7px] rounded-full",
                widgetState.tone === "ok"
                  ? "bg-[#3fd9a8] shadow-[0_0_0_3px_rgba(63,217,168,0.18)]"
                  : "bg-[#8a8a93]",
              )}
            />
            {widgetState.label}
          </div>
          {/* 한도는 색만으로 알리지 않는다 — 숫자를 함께 적는다 (WCAG 2.1 AA) */}
          <div className="mt-2.5 h-1 overflow-hidden rounded-[3px] bg-white/12">
            <span className="block h-full rounded-[3px] bg-mark-bright" style={{ width: `${usedPercent}%` }} />
          </div>
          <div className="mt-1.5 flex justify-between font-mono text-[10.5px] text-[#8b8b94]">
            <span>
              {usage ? `${usage.convCount.toLocaleString()} / ${usage.convLimit.toLocaleString()} 대화` : "—"}
            </span>
            <span>{usage ? `${usedPercent}%` : ""}</span>
          </div>
        </div>

        <div className="mt-2.5 flex items-center gap-2 px-1 text-[11.5px] text-[#8b8b94]">
          <span className="min-w-0 flex-1 truncate">{data?.member?.email ?? "—"}</span>
          <LogoutButton className="shrink-0" />
        </div>
      </div>
    </aside>
  );
}
