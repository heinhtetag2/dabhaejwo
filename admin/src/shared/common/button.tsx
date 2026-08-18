// tenant/shared/common/button.tsx 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
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

/*
 * 곡률만 revamp 로 옮겼다 — **세로 여백과 글자 크기는 한 픽셀도 건드리지 않았다.**
 * 그 둘이 높이를 정하고, 높이는 `control.tsx` 와 맞춰져 있다(같은 줄의 입력과 버튼이
 * 어긋나던 문제를 그렇게 없앴다). 모양을 바꾸려다 그 합의를 깨면 화면 전체가 다시 어긋난다.
 */
const SIZE: Record<Size, string> = {
  /** 공개 영역의 주요 행동. 손가락으로 누르는 크기이고, 화면에서 다음 할 일이 분명해야 한다. */
  lg: "px-6 py-[14px] text-[15.5px] rounded-full font-semibold",
  md: "px-4 py-[7.5px] text-[13.5px] rounded-full",
  sm: "px-3 py-[4.5px] text-[12.5px] rounded-full",
};

const BASE =
  "inline-flex items-center gap-1.5 border font-medium whitespace-nowrap transition-colors";

/** `Button`/`LinkButton` 과 같은 클래스 조합을 계산만 한다 — 앵커가 아닌 다른 요소에 같은
 *  모양을 입혀야 할 때 쓴다. BASE/VARIANT/SIZE 를 export 하지 않는 이유는 소비자가 조합
 *  순서까지 신경 쓰게 만들지 않기 위해서다. (tenant 쪽 사용처: `shared/ui/signup-cta-link.tsx`) */
export function buttonClassName({
  variant = "default",
  size = "md",
  className,
}: {
  variant?: Variant;
  size?: Size;
  className?: string;
}): string {
  return cn(BASE, VARIANT[variant], SIZE[size], className);
}

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
