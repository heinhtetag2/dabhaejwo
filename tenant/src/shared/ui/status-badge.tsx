import { cn } from "@/shared/lib/cn";

export type Tone = "ok" | "warn" | "error" | "idle";

const TONE: Record<Tone, string> = {
  ok: "bg-seal-soft text-seal",
  warn: "bg-mark-soft text-[#8a6a00]",
  error: "bg-brick-soft text-brick",
  idle: "bg-line-2 text-slate",
};

/**
 * 상태 배지.
 *
 * <p><b>색만으로 상태를 구분하지 않는다.</b> 항상 텍스트 라벨을 함께 낸다 (WCAG 2.1 AA).
 * 점은 장식이므로 aria-hidden 이다.
 */
export function StatusBadge({
  tone,
  label,
  className,
}: {
  tone: Tone;
  label: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2 py-[2.5px] font-mono text-[11px] font-medium tracking-[0.03em]",
        TONE[tone],
        className,
      )}
    >
      <i aria-hidden className="size-[5px] rounded-full bg-current" />
      {label}
    </span>
  );
}
