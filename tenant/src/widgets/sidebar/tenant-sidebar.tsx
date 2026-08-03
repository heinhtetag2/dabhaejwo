"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { NAV_GROUPS, ROUTES } from "@/shared/config/routes";
import { useAuthStore } from "@/shared/lib/auth-store";
import { cn } from "@/shared/lib/cn";

export function TenantSidebar() {
  const pathname = usePathname();
  const member = useAuthStore((state) => state.member);

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
          {member?.tenantName ?? "—"}
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
    </aside>
  );
}
