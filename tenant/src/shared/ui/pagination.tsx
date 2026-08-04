"use client";

import { Button } from "@/shared/common/button";

/**
 * 페이지 이동. 총 건수를 함께 보여준다 —
 * "몇 페이지인지"보다 "몇 건인지"가 업체에게 먼저 필요한 정보다.
 */
export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (next: number) => void;
}) {
  if (totalPages <= 1) {
    return (
      <p className="px-3.5 py-3 text-[11.5px] text-slate-2">
        전체 {totalElements.toLocaleString()}건
      </p>
    );
  }

  return (
    <nav
      aria-label="페이지 이동"
      className="flex items-center gap-3 border-t border-line-2 px-3.5 py-2.5"
    >
      <span className="text-[11.5px] text-slate-2">
        전체 {totalElements.toLocaleString()}건
      </span>
      <span className="ml-auto flex items-center gap-2">
        <Button size="sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>
          이전
        </Button>
        <span className="tabular text-[12px] text-slate">
          {page + 1} / {totalPages}
        </span>
        <Button size="sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
          다음
        </Button>
      </span>
    </nav>
  );
}
