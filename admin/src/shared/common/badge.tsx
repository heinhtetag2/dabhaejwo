import type { ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

export type BadgeTone = "ok" | "warn" | "error" | "idle" | "info";

const TONE: Record<BadgeTone, string> = {
  ok: "bg-seal-soft text-seal",
  warn: "bg-mark-soft text-[#8a6a00]",
  error: "bg-brick-soft text-brick",
  idle: "bg-line-2 text-slate",
  info: "bg-plum-soft text-plum",
};

/**
 * 상태 표시. 색만으로 구분하지 않고 항상 텍스트 라벨을 함께 낸다 (WCAG 2.1 AA).
 */
export function Badge({
  tone = "idle",
  dot = true,
  children,
  className,
}: {
  tone?: BadgeTone;
  dot?: boolean;
  children: ReactNode;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2 py-[2.5px]",
        "font-mono text-[11px] font-medium tracking-[0.03em]",
        TONE[tone],
        className,
      )}
    >
      {dot ? <i aria-hidden className="size-[5px] rounded-full bg-current" /> : null}
      {children}
    </span>
  );
}
