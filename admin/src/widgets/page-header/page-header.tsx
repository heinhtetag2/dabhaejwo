import type { ReactNode } from "react";

export function PageHeader({
  title,
  description,
  guide,
  actions,
}: {
  title: string;
  description?: string;
  /**
   * 제목 옆 물음표 버튼. 이 화면을 어떻게 쓰는지 설명하는 모달을 연다.
   *
   * <p>본문에 안내를 깔지 않는 이유 — 처음 한 번 읽고 나면 그 뒤로는 매번 지나쳐야 하는
   * 여백이 된다. 필요할 때만 열어보게 한다.
   */
  guide?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className="mb-5 flex flex-wrap items-center gap-4">
      <div>
        <h1 className="flex items-center gap-1.5 text-[19px] font-semibold tracking-[-0.02em]">
          {title}
          {guide}
        </h1>
        {description ? (
          <p className="mt-px text-[12.5px] text-slate-2">{description}</p>
        ) : null}
      </div>
      {actions ? <div className="ml-auto flex items-center gap-2">{actions}</div> : null}
    </header>
  );
}
