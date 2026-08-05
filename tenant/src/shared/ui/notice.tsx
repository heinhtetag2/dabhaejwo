import type { ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 화면 안내. 성공·실패를 색과 <b>문구</b>로 함께 알린다.
 *
 * <p>토스트를 쓰지 않는 이유는 사라지기 때문이다 — "아직 연결되지 않았습니다" 같은
 * 안내는 업체가 다시 읽을 수 있어야 한다.
 */
export function Notice({
  tone = "info",
  size = "sm",
  children,
  className,
}: {
  tone?: "info" | "warn" | "error";
  /** 공개 영역은 읽는 화면이라 한 단계 크다. 대시보드는 정보 밀도가 우선이므로 기본은 작다. */
  size?: "sm" | "md";
  children: ReactNode;
  className?: string;
}) {
  return (
    <p
      role="status"
      className={cn(
        "leading-relaxed",
        size === "sm" ? "rounded-[7px] px-3 py-2.5 text-[12.5px]" : "rounded-block px-4 py-4 text-[14px]",
        tone === "error" && "bg-brick-soft text-brick",
        tone === "warn" && "bg-mark-soft text-mark-ink",
        tone === "info" && "bg-line-2 text-slate",
        className,
      )}
    >
      {children}
    </p>
  );
}
