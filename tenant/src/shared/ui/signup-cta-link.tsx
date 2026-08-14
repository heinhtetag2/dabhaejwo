"use client";

import Link from "next/link";
import type { ReactNode } from "react";

import { buttonClassName } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import { useSignupCtaClick } from "@/shared/lib/use-signup-cta-click";

/**
 * "무료로 시작" 류 CTA. 실제로는 `<a href="/signup">` 인 채로 있는다(`LinkButton` 과 같은
 * 이유 — 이동은 앵커여야 한다) — 수정 없는 좌클릭만 `useSignupCtaClick` 이 가로채 모달을
 * 열고, Ctrl/Cmd/Shift/Alt+클릭·중클릭은 그대로 `/signup` 페이지로 보낸다.
 *
 * <p>헤더·랜딩·요금제처럼 `LinkButton` 모양이 필요한 곳은 `variant`/`size` 를 준다.
 * 푸터처럼 평문 링크가 필요한 곳은 `variant`/`size` 를 비우고 `className` 만 준다 —
 * 두 모양을 하나로 합치면 `BASE`(테두리·패딩)가 평문 링크에도 섞여 들어간다.
 *
 * <p>이 컴포넌트를 `shared/ui` 에 두는 이유: 헤더(widgets)·랜딩·요금제(여러 feature)가
 * 함께 쓴다 — fsd-rules: 둘 이상의 feature가 같이 쓰는 UI는 shared/ui로.
 *
 * <p>헤더·요금제(v2)처럼 부모가 Server Component 인 자리에서도 `useSignupCtaClick` 훅을
 * 쓸 수 있는 이유는 이 컴포넌트 자체가 `"use client"` 섬(island)이기 때문이다.
 */
export function SignupCtaLink({
  variant,
  size,
  className,
  children,
}: {
  variant?: "default" | "primary" | "accent" | "danger" | "ghost";
  size?: "lg" | "md" | "sm";
  className?: string;
  children: ReactNode;
}) {
  const handleClick = useSignupCtaClick();
  const resolvedClassName =
    variant === undefined && size === undefined ? className : buttonClassName({ variant, size, className });

  return (
    <Link href={ROUTES.signup} className={resolvedClassName} onClick={handleClick}>
      {children}
    </Link>
  );
}
