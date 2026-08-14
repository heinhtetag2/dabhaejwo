"use client";

import Link from "next/link";
import type { ReactNode } from "react";

import { buttonClassName } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import { useLoginCtaClick } from "@/shared/lib/use-login-cta-click";

/**
 * "로그인" CTA — `shared/ui/signup-cta-link.tsx` 와 같은 패턴. 실제로는
 * `<a href="/login">` 인 채로 있는다 — 수정 없는 좌클릭만 `useLoginCtaClick` 이 가로채
 * 모달을 열고, Ctrl/Cmd/Shift/Alt+클릭·중클릭은 그대로 `/login` 페이지로 보낸다.
 */
export function LoginCtaLink({
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
  const handleClick = useLoginCtaClick();
  const resolvedClassName =
    variant === undefined && size === undefined ? className : buttonClassName({ variant, size, className });

  return (
    <Link href={ROUTES.login} className={resolvedClassName} onClick={handleClick}>
      {children}
    </Link>
  );
}
