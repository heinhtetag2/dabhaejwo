import type { ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 공개 영역의 섹션 리듬.
 *
 * <p>랜딩·요금제·약관이 함께 쓴다. 페이지마다 여백을 손으로 맞추면 스크롤할 때
 * 리듬이 어긋나고, 그 어긋남이 "덜 만든 화면"처럼 보인다.
 *
 * <p>구분선을 쓰지 않는다 — 여백이 충분하면 선이 필요 없고, 선이 많을수록 화면이 시끄럽다.
 */
export function PublicSection({
  eyebrow,
  title,
  description,
  tone = "plain",
  children,
  className,
}: {
  eyebrow?: string;
  title?: string;
  description?: ReactNode;
  /** {@code fill} 은 옅은 면을 깐다. 흰 화면이 길게 이어질 때 구간을 나눈다. */
  tone?: "plain" | "fill";
  children?: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn(tone === "fill" && "bg-fill", className)}>
      <div className="mx-auto max-w-[1080px] px-5 py-20 sm:py-24 lg:py-28">
        {eyebrow ? (
          <p className="text-[13px] font-semibold tracking-[-0.01em] text-slate-2">{eyebrow}</p>
        ) : null}
        {title ? (
          <h2 className="mt-2.5 text-[26px] leading-[1.35] font-bold tracking-[-0.035em] text-balance sm:text-[32px]">
            {title}
          </h2>
        ) : null}
        {description ? (
          <div className="mt-4 max-w-[620px] text-[15.5px] leading-[1.75] text-slate">
            {description}
          </div>
        ) : null}
        {children ? <div className={cn(title || description ? "mt-11" : "")}>{children}</div> : null}
      </div>
    </section>
  );
}

/**
 * 테두리 없는 카드. 면으로 구분한다.
 *
 * <p>흰 배경 위에서는 옅은 면({@code fill}), 면 위에서는 흰색({@code card})을 쓴다.
 * 그래서 배경색을 쓰는 쪽이 정한다 — 카드가 스스로 정하면 섹션을 옮길 때마다 어긋난다.
 */
export function PublicCard({
  as: Tag = "div",
  className,
  children,
}: {
  as?: "div" | "li" | "article";
  className?: string;
  children: ReactNode;
}) {
  return (
    <Tag className={cn("rounded-panel p-7 sm:p-8", className)}>{children}</Tag>
  );
}
