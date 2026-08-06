"use client";

import { useState, type ReactNode } from "react";

import { Modal } from "@/shared/ui/modal";

/**
 * 화면 사용 가이드.
 *
 * <p>제목 옆 물음표를 누르면 모달로 열린다. 본문에 안내를 깔지 않는 이유 — 처음 한 번
 * 읽고 나면 그 뒤로는 매번 지나쳐야 하는 여백이 된다.
 *
 * <p>확인 버튼이 없는 <b>읽는 모달</b>이다({@code onConfirm} 을 넘기지 않는다).
 * 아무 일도 하지 않는 "확인"은 무엇이 확정되는지 헷갈리게 한다.
 */
export function GuideButton({ title, children }: { title: string; children: ReactNode }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label={`${title} 사용 가이드 열기`}
        className="grid size-[18px] shrink-0 place-items-center rounded-full border border-line bg-card font-mono text-[11px] leading-none font-semibold text-slate transition-colors hover:border-ink-3 hover:text-ink"
      >
        ?
      </button>

      <Modal open={open} onOpenChange={setOpen} title={title} size="lg">
        <div className="space-y-4 text-[13px] leading-relaxed text-ink">{children}</div>
      </Modal>
    </>
  );
}

/** 가이드 안의 한 절. 제목과 본문의 간격을 화면마다 다르게 잡지 않기 위해 둔다. */
export function GuideSection({ heading, children }: { heading: string; children: ReactNode }) {
  return (
    <section>
      <h3 className="mb-1.5 text-[13.5px] font-semibold">{heading}</h3>
      <div className="space-y-1.5 text-[12.5px] leading-relaxed text-slate">{children}</div>
    </section>
  );
}

/** 절 안의 항목 목록. */
export function GuideList({ items }: { items: ReactNode[] }) {
  return (
    <ul className="space-y-1">
      {items.map((item, index) => (
        <li key={index} className="flex gap-2">
          <span aria-hidden className="mt-[7px] size-[3px] shrink-0 rounded-full bg-slate-2" />
          <span className="min-w-0">{item}</span>
        </li>
      ))}
    </ul>
  );
}

/** 되돌릴 수 없는 것·자주 나는 사고를 눈에 띄게 남긴다. */
export function GuideWarning({ children }: { children: ReactNode }) {
  return (
    <p className="rounded-lg bg-mark-soft px-3 py-2.5 text-[12.5px] leading-relaxed text-[#8a6a00]">
      {children}
    </p>
  );
}
