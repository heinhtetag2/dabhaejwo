"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { LogoutButton } from "@/features/auth/logout";
import { Eyebrow } from "@/shared/common/card";
import { NAV_GROUPS } from "@/shared/config/routes";
import { can, useAuthStore } from "@/shared/lib/auth-store";
import { cn } from "@/shared/lib/cn";

const ROLE_LABEL: Record<string, string> = {
  OPS_ADMIN: "운영 관리자",
  CS: "CS 담당",
  SALES: "영업 담당",
  DEV: "개발",
};

export function OpsSidebar() {
  const pathname = usePathname();
  const operator = useAuthStore((state) => state.operator);

  return (
    <aside className="sticky top-0 flex h-dvh w-[230px] shrink-0 flex-col bg-ink text-[#c9d4dc]">
      <div className="border-b border-white/8 px-5 pt-5 pb-4">
        <div className="flex items-center gap-2.5">
          <span className="grid size-[22px] place-items-center rounded-md bg-seal font-mono text-[11px] font-semibold text-white">
            OPS
          </span>
          <span className="font-semibold tracking-[-0.01em] text-white">답해줘 운영</span>
        </div>
        <div className="mt-[5px] font-mono text-[10.5px] tracking-[0.08em] text-[#6c7d8b]">
          PRODUCTION
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3.5">
        {NAV_GROUPS.map((group) => {
          // 권한 없는 항목은 숨긴다. 그룹이 통째로 비면 제목도 내린다 —
          // 빈 제목만 남으면 "뭔가 있는데 안 보인다"로 읽힌다.
          const visible = group.items.filter(
            (item) => !item.permission || can(operator, item.permission),
          );
          if (visible.length === 0) {
            return null;
          }
          return (
            <div key={group.label} className="mb-[18px]">
              <Eyebrow className="block px-2 pb-[7px] text-[10px] text-[#6c7d8b]">
                {group.label}
              </Eyebrow>
              {visible.map((item) => {
                const active = pathname.startsWith(item.href);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "mb-px flex w-full items-center gap-2.5 rounded-[7px] px-2.5 py-2 text-[13.5px] transition-colors",
                      active
                        ? "bg-seal/30 font-medium text-white"
                        : "text-[#b9c6cf] hover:bg-white/7 hover:text-white",
                    )}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </div>
          );
        })}
      </nav>

      <div className="flex items-center gap-2 border-t border-white/8 p-3 text-xs text-[#8fa3b0]">
        <div className="min-w-0 flex-1">
          <b className="block truncate text-[12.5px] font-medium text-white">
            {operator?.name ?? "—"}
          </b>
          {operator ? ROLE_LABEL[operator.role] : "로그인이 필요합니다"}
        </div>
        <LogoutButton className="shrink-0" />
      </div>
    </aside>
  );
}
