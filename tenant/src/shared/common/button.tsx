// admin/shared/common/button.tsx 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
// (kickoff-prompt.md §3) 복제해 두었다. 한쪽을 고치면 다른 쪽도 같은 커밋에서 맞춘다.

import Link from "next/link";
import type { ButtonHTMLAttributes, ComponentProps } from "react";

import { cn } from "@/shared/lib/cn";

type Variant = "default" | "primary" | "accent" | "danger" | "ghost";
type Size = "lg" | "md" | "sm";

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
  /** 공개 영역의 주요 행동. 손가락으로 누르는 크기이고, 화면에서 다음 할 일이 분명해야 한다. */
  lg: "px-6 py-[14px] text-[15.5px] rounded-btn font-semibold",
  md: "px-3.5 py-[7.5px] text-[13.5px] rounded-[7px]",
  sm: "px-2.5 py-[4.5px] text-[12.5px] rounded-md",
};

const BASE =
  "inline-flex items-center gap-1.5 border font-medium whitespace-nowrap transition-colors";

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
      className={cn(BASE, "disabled:cursor-not-allowed disabled:opacity-50", VARIANT[variant], SIZE[size], className)}
      {...rest}
    />
  );
}

/**
 * 버튼처럼 보이는 링크.
 *
 * <p>이동을 {@code onClick} + {@code router.push} 로 하면 새 탭 열기·주소 복사가 막히고
 * 스크린 리더에도 버튼으로 읽힌다. 이동은 앵커여야 한다.
 */
export function LinkButton({
  variant = "default",
  size = "md",
  className,
  ...rest
}: ComponentProps<typeof Link> & { variant?: Variant; size?: Size }) {
  return <Link className={cn(BASE, VARIANT[variant], SIZE[size], className)} {...rest} />;
}
