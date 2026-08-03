import { cn } from "@/shared/lib/cn";

/**
 * 원가율 막대 — 이 콘솔의 시그니처.
 *
 * 규칙 (tenant-plan.md §4.1.4):
 *   · 경고선 지점에 기준선을 표시한다
 *   · 경고선 미만 초록 / 경고선~99% 노랑 / 100% 이상 빨강
 *   · 100%를 넘어도 막대는 100%에서 멈추고 초과분은 숫자로만 표기한다
 *     — 막대가 넘쳐 흐르면 200%와 400%가 똑같아 보인다
 */
export function CostRatioBar({
  percent,
  warnThreshold = 70,
  className,
}: {
  percent: number;
  warnThreshold?: number;
  className?: string;
}) {
  const level = percent >= 100 ? "loss" : percent >= warnThreshold ? "warn" : "normal";
  const label =
    level === "loss" ? "손실" : level === "warn" ? "주의" : "정상";

  return (
    <div className={cn("flex min-w-[132px] items-center gap-2.5", className)}>
      <div
        className="relative h-1.5 flex-1 overflow-hidden rounded bg-line-2"
        role="img"
        // 색만으로 구분되지 않도록 스크린리더에는 판정을 말로 전달한다
        aria-label={`원가율 ${percent}% (${label})`}
      >
        <i
          className={cn(
            "block h-full rounded",
            level === "loss" && "bg-brick",
            level === "warn" && "bg-mark",
            level === "normal" && "bg-seal",
          )}
          style={{ width: `${Math.min(percent, 100)}%` }}
        />
        <span
          aria-hidden
          className="absolute -top-0.5 h-2.5 w-px bg-slate-2/50"
          style={{ left: `${warnThreshold}%` }}
        />
      </div>
      <span
        className={cn(
          "tabular w-9 text-right text-[11.5px]",
          level === "loss" ? "font-semibold text-brick" : "text-slate",
        )}
      >
        {percent}%
      </span>
    </div>
  );
}
