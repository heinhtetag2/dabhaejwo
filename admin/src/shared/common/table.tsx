import type { ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 표 원자. 운영 콘솔은 거의 모든 화면이 표라서 헤더·셀 스타일이 흩어지면 금방 어긋난다.
 *
 * 도메인 지식이 없으므로 shared 에 둔다. 둘 이상의 feature 가 함께 쓴다.
 */

export function Table({ children, className }: { children: ReactNode; className?: string }) {
  return (
    // 태블릿까지 지원한다. 좁아지면 표만 가로 스크롤하고 페이지 본문은 넘치지 않는다.
    <div className={cn("overflow-x-auto px-1 pt-3.5 pb-1", className)}>
      <table className="w-full border-collapse">{children}</table>
    </div>
  );
}

export function Th({ className, children }: { className?: string; children?: ReactNode }) {
  return (
    <th
      className={cn(
        "border-b border-line-2 px-3.5 pb-2.5 text-left whitespace-nowrap",
        "font-mono text-[10.5px] font-medium tracking-[0.09em] text-slate-2 uppercase",
        className,
      )}
    >
      {children}
    </th>
  );
}

export function Td({
  className,
  children,
  colSpan,
}: {
  className?: string;
  children?: ReactNode;
  colSpan?: number;
}) {
  return (
    <td
      colSpan={colSpan}
      className={cn("border-b border-line-2 px-3.5 py-2.5 align-middle text-[13.5px]", className)}
    >
      {children}
    </td>
  );
}

/** 업체명 + 부제(도메인·요금제) 두 줄. 표 어디서나 같은 모양이어야 한다. */
export function TitleCell({ title, sub }: { title: ReactNode; sub?: ReactNode }) {
  return (
    <>
      <div className="font-medium">{title}</div>
      {sub ? <div className="tabular text-[11.5px] text-slate-2">{sub}</div> : null}
    </>
  );
}
