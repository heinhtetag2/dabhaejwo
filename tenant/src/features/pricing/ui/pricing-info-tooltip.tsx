"use client";

import { HelpCircle } from "lucide-react";
import { useEffect, useRef, useState } from "react";

/**
 * 비교표 행 라벨 옆 정보 아이콘. 눌러야 뜬다(호버 아님) — 터치 기기에서도 같게 동작해야 한다.
 *
 * <p>표 전체(`pricing-view-v2.tsx`)는 Server Component 로 둔다. 이 조각만 클릭 상태가
 * 필요해 따로 뗐다 — `language-switch.tsx` 와 같은 이유다.
 */
export function InfoTooltip({ text }: { text: string }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    if (!open) return;

    function onPointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  return (
    <span ref={rootRef} className="relative inline-flex">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-label="설명 보기"
        className="text-black/30 transition-colors hover:text-black/60"
      >
        <HelpCircle aria-hidden className="size-4" />
      </button>

      {open ? (
        <span
          role="tooltip"
          className="absolute top-full left-1/2 z-10 mt-2 w-[220px] -translate-x-1/2 rounded-[10px] border border-black/[0.08] bg-white p-3 text-left text-[13px] leading-[1.5] font-normal text-black/70 shadow-lg"
        >
          {text}
        </span>
      ) : null}
    </span>
  );
}
