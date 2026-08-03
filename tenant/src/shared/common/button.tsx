// admin/shared/common/button.tsx 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
// (kickoff-prompt.md §3) 복제해 두었다. 한쪽을 고치면 다른 쪽도 같은 커밋에서 맞춘다.

import type { ButtonHTMLAttributes } from "react";

import { cn } from "@/shared/lib/cn";

type Variant = "default" | "primary" | "accent" | "danger" | "ghost";
type Size = "md" | "sm";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
}

const VARIANT: Record<Variant, string> = {
  default: "border-line bg-card hover:bg-line-2/60 hover:border-ink-3/40",
  primary: "border-ink bg-ink text-white hover:bg-ink-2 hover:border-ink-2",
  accent: "border-seal bg-seal text-white hover:brightness-110",
  danger: "border-brick/40 text-brick hover:bg-brick-soft",
  ghost: "border-transparent bg-transparent text-slate hover:bg-line-2",
};

const SIZE: Record<Size, string> = {
  md: "px-3.5 py-[7.5px] text-[13.5px] rounded-[7px]",
  sm: "px-2.5 py-[4.5px] text-[12.5px] rounded-md",
};

export function Button({
  variant = "default",
  size = "md",
  className,
  type = "button",
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        "inline-flex items-center gap-1.5 border font-medium whitespace-nowrap transition-colors",
        "disabled:cursor-not-allowed disabled:opacity-50",
        VARIANT[variant],
        SIZE[size],
        className,
      )}
      {...rest}
    />
  );
}
