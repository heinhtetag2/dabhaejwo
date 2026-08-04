"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { useAppContextQuery } from "@/entities/auth/session";
import { NAV_GROUPS, ROUTES } from "@/shared/config/routes";
import { cn } from "@/shared/lib/cn";

export function TenantSidebar() {
  const pathname = usePathname();
  const { data } = useAppContextQuery();
  const usage = data?.usage;
  const usedPercent =
    usage && usage.convLimit > 0
      ? Math.min(100, Math.round((usage.convCount / usage.convLimit) * 100))
      : 0;

  return (
    <aside className="sticky top-0 flex h-dvh w-[236px] shrink-0 flex-col bg-ink text-[#c9d4dc]">
      <div className="border-b border-white/8 px-5 pt-5.5 pb-4.5">
        <div className="flex items-center gap-2.5">
          <span className="grid size-[22px] place-items-center rounded-md bg-mark font-mono text-xs font-bold text-ink">
            A
          </span>
          <span className="font-semibold tracking-[-0.01em] text-white">답해줘</span>
        </div>
        <div className="mt-3.5 rounded-lg bg-white/6 px-2.5 py-2 text-[13px] text-[#dde6eb]">
          {data?.tenant.name ?? "—"}
          <span className="block font-mono text-[10.5px] tracking-[0.06em] text-[#7e8f9c]">
            {data?.tenant.primaryDomain ?? ""}
          </span>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3.5">
        {NAV_GROUPS.map((group) => (
          <div key={group.label} className="mb-[18px]">
            <span className="block px-2 pb-[7px] font-mono text-[10px] tracking-[0.11em] text-[#6c7d8b] uppercase">
              {group.label}
            </span>
            {group.items.map((item) => {
              const active =
                item.href === ROUTES.home
                  ? pathname === ROUTES.home
                  : pathname.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "mb-px flex w-full items-center gap-2.5 rounded-[7px] px-2.5 py-2 text-[13.5px] transition-colors",
                    active
                      ? "bg-mark/15 font-medium text-white"
                      : "text-[#b9c6cf] hover:bg-white/7 hover:text-white",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      <div className="border-t border-white/8 p-3.5">
        <div className="rounded-lg bg-white/5 px-3 py-[11px]">
          <div className="flex items-center gap-[7px] text-[12.5px] text-[#dde6eb]">
            <span
              aria-hidden
              className="size-[7px] rounded-full bg-[#3fd9a8] shadow-[0_0_0_3px_rgba(63,217,168,0.18)]"
            />
            사이트에서 작동 중
          </div>
          {/* 한도는 색만으로 알리지 않는다 — 숫자를 함께 적는다 (WCAG 2.1 AA) */}
          <div className="mt-2.5 h-1 overflow-hidden rounded-[3px] bg-white/12">
            <span className="block h-full rounded-[3px] bg-mark" style={{ width: `${usedPercent}%` }} />
          </div>
          <div className="mt-1.5 flex justify-between font-mono text-[10.5px] text-[#7e8f9c]">
            <span>
              {usage ? `${usage.convCount.toLocaleString()} / ${usage.convLimit.toLocaleString()} 대화` : "—"}
            </span>
            <span>{usage ? `${usedPercent}%` : ""}</span>
          </div>
        </div>
      </div>
    </aside>
  );
}
