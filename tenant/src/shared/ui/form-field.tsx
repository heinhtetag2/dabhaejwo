import type { ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 공개 영역 폼의 입력 한 칸.
 *
 * <p>로그인과 가입이 같은 모양을 따로 갖고 있었다. 한쪽만 고치면 두 화면이 어긋나므로
 * 하나로 모은다.
 *
 * <p><b>오류가 있으면 힌트를 감춘다.</b> 둘을 같이 보여주면 어느 쪽이 지금 해야 할 말인지
 * 흐려진다. 오류는 색만으로 알리지 않고 문장으로 말한다 (WCAG 2.1 AA).
 */
export function FormField({
  id,
  label,
  hint,
  error,
  children,
}: {
  id: string;
  label: string;
  hint?: ReactNode;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="mb-5">
      <label htmlFor={id} className="mb-2 block text-[13.5px] font-semibold">
        {label}
      </label>
      {children}
      {error ? (
        <p role="alert" className="mt-2 text-[12.5px] leading-relaxed text-brick">
          {error}
        </p>
      ) : hint ? (
        <p className="mt-2 text-[12.5px] leading-relaxed text-slate-2">{hint}</p>
      ) : null}
    </div>
  );
}

/**
 * 입력 상자.
 *
 * <p>테두리를 옅게 두고 면을 깐다. 포커스에서만 테두리가 또렷해진다 —
 * 지금 어디에 쓰고 있는지가 화면에서 가장 분명해야 한다.
 */
export function fieldInputClass(invalid?: boolean): string {
  return cn(
    "w-full rounded-block bg-fill px-4 py-[13px] text-[15px] transition-colors",
    "border outline-none placeholder:text-slate-2",
    "focus:border-ink focus:bg-card",
    invalid ? "border-brick/60" : "border-transparent",
  );
}
