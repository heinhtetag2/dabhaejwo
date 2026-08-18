import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";
import { useId } from "react";

import { cn } from "@/shared/lib/cn";

const CONTROL =
  "w-full rounded-[9px] border border-line bg-card px-[11px] py-[8.5px] text-[13.5px] " +
  "focus:border-ink-3 focus:outline-none focus:ring-[3px] focus:ring-ink/6";

/**
 * 라벨 + 입력 + 도움말 한 벌.
 *
 * 라벨을 {@code htmlFor} 로 묶는다 — 프로토타입은 라벨과 입력이 떨어져 있어
 * 스크린 리더에서 무엇을 입력하는지 알 수 없다 (docs/IMPROVEMENTS.md 접근성 부채).
 */
export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string;
  hint?: ReactNode;
  error?: string | null;
  children: (id: string) => ReactNode;
}) {
  const id = useId();
  return (
    <div className="mb-4">
      <label htmlFor={id} className="mb-1.5 block text-[12.5px] font-medium">
        {label}
      </label>
      {children(id)}
      {hint ? <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">{hint}</p> : null}
      {error ? <p className="mt-1.5 text-[11.5px] text-brick">{error}</p> : null}
    </div>
  );
}

export function TextInput({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(CONTROL, className)} {...rest} />;
}

export function NumberInput({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input type="number" className={cn(CONTROL, "tabular", className)} {...rest} />;
}

export function TextArea({ className, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn(CONTROL, "min-h-[74px] resize-y leading-relaxed", className)} {...rest} />;
}

export function Select({ className, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={cn(CONTROL, className)} {...rest} />;
}
