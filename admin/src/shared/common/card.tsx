import type { ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

export function Card({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <section className={cn("rounded-card border border-line bg-card", className)}>
      {children}
    </section>
  );
}

export function CardHeader({ title, aside }: { title: ReactNode; aside?: ReactNode }) {
  return (
    <header className="flex items-center gap-3 border-b border-line-2 px-[18px] py-3.5">
      <h3 className="text-[14.5px] font-semibold tracking-[-0.01em]">{title}</h3>
      {aside ? <div className="ml-auto flex items-center gap-2">{aside}</div> : null}
    </header>
  );
}

export function CardBody({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn("p-[18px]", className)}>{children}</div>;
}

/** 대문자 라벨. 프로토타입의 .eyebrow. */
export function Eyebrow({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <span
      className={cn(
        "font-mono text-[11px] font-medium tracking-[0.11em] text-slate-2 uppercase",
        className,
      )}
    >
      {children}
    </span>
  );
}

/** 지표 카드. */
export function Stat({
  label,
  value,
  detail,
  tone = "neutral",
}: {
  label: string;
  value: ReactNode;
  detail?: ReactNode;
  tone?: "neutral" | "up" | "down";
}) {
  return (
    <div className="rounded-card border border-line bg-card px-4 py-[15px]">
      <Eyebrow>{label}</Eyebrow>
      <div className="tabular mt-[7px] text-2xl leading-none font-semibold tracking-[-0.03em]">
        {value}
      </div>
      {detail ? (
        <div
          className={cn(
            "tabular mt-1.5 text-[11.5px]",
            tone === "up" && "text-seal",
            tone === "down" && "text-brick",
            tone === "neutral" && "text-slate-2",
          )}
        >
          {detail}
        </div>
      ) : null}
    </div>
  );
}
