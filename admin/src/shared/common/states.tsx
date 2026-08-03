import type { ReactNode } from "react";

import { Button } from "@/shared/common/button";

/**
 * 목록·상세 화면은 로딩·에러·빈 상태 3종을 모두 처리한다 (frontend-rules).
 * 하나라도 빠지면 사용자는 화면이 멈춘 건지 데이터가 없는 건지 알 수 없다.
 */

export function LoadingState({ label = "불러오는 중" }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 px-6 py-16 text-[13px] text-slate-2">
      <i
        aria-hidden
        className="size-3.5 animate-spin rounded-full border-2 border-line border-t-seal"
      />
      {label}
    </div>
  );
}

export function EmptyState({ message, action }: { message: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-16 text-center">
      <p className="text-[13.5px] text-slate">{message}</p>
      {action}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-16 text-center">
      <p className="text-[13.5px] text-brick">{message}</p>
      {onRetry ? (
        <Button size="sm" onClick={onRetry}>
          다시 시도
        </Button>
      ) : null}
    </div>
  );
}
