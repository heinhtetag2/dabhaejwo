"use client";

import type { Faq } from "@/entities/chatbot/faq";
import { cn } from "@/shared/lib/cn";

/**
 * 공통 질문 목록. 순서 조정과 노출 토글이 여기 붙는다.
 *
 * <p>순서는 위아래 버튼으로 바꾼다. 드래그는 키보드로 조작할 수 없어
 * 접근성 요구(키보드 조작)를 만족하지 못한다.
 */
export function FaqList({
  faqs,
  selectedId,
  editable,
  onSelect,
  onMove,
  onToggleShown,
}: {
  faqs: Faq[];
  selectedId: string | null;
  editable: boolean;
  onSelect: (faq: Faq) => void;
  onMove: (index: number, direction: -1 | 1) => void;
  onToggleShown: (faq: Faq) => void;
}) {
  return (
    <ul>
      {faqs.map((faq, index) => (
        <li
          key={faq.id}
          className={cn(
            "flex items-center gap-3 border-b border-line-2 px-3.5 py-2.5 last:border-b-0",
            faq.id === selectedId && "bg-mark-soft/40",
          )}
        >
          <span className="flex shrink-0 flex-col">
            <OrderButton
              label={`${faq.question} 위로`}
              disabled={!editable || index === 0}
              onClick={() => onMove(index, -1)}
            >
              ▲
            </OrderButton>
            <OrderButton
              label={`${faq.question} 아래로`}
              disabled={!editable || index === faqs.length - 1}
              onClick={() => onMove(index, 1)}
            >
              ▼
            </OrderButton>
          </span>

          <button
            type="button"
            onClick={() => onSelect(faq)}
            className="min-w-0 flex-1 truncate text-left text-[13.5px]"
          >
            <span className="tabular mr-2 text-[11.5px] text-slate-2">
              {String(index + 1).padStart(2, "0")}
            </span>
            {faq.question}
          </button>

          <span className="tabular w-14 shrink-0 text-right text-[11.5px] text-slate-2">
            {faq.hitCount.toLocaleString()}
          </span>

          {/* 노출은 색만으로 알리지 않는다 — 스위치에 상태 텍스트를 붙인다 */}
          <button
            type="button"
            role="switch"
            aria-checked={faq.shown}
            aria-label={`${faq.question} 버튼 노출`}
            disabled={!editable}
            onClick={() => onToggleShown(faq)}
            className={cn(
              "relative h-5 w-9 shrink-0 rounded-full transition-colors disabled:opacity-50",
              faq.shown ? "bg-seal" : "bg-line",
            )}
          >
            <span
              aria-hidden
              className={cn(
                "absolute top-0.5 size-4 rounded-full bg-white transition-all",
                faq.shown ? "left-4.5" : "left-0.5",
              )}
            />
          </button>
        </li>
      ))}
    </ul>
  );
}

function OrderButton({
  label,
  disabled,
  onClick,
  children,
}: {
  label: string;
  disabled: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className="px-1 text-[9px] leading-tight text-slate-2 transition-colors hover:text-ink disabled:opacity-25 disabled:hover:text-slate-2"
    >
      {children}
    </button>
  );
}
