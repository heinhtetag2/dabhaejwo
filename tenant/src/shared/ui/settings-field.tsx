"use client";

import type { ReactNode } from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 설정 화면의 입력 한 칸과 토글.
 *
 * <p>말투와 위젯 관리가 같은 모양을 써야 해서 여기 둔다. 한쪽 화면 안에 두면 다른 화면이
 * 그것을 가져다 쓰게 되고(features → features), 그게 실제로 났던 사고다 (fsd-rules).
 */
export function SettingsField({
  id,
  label,
  hint,
  children,
}: {
  id: string;
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div className="mb-4">
      <label htmlFor={id} className="mb-1.5 block text-[12.5px] font-medium">
        {label}
      </label>
      {children}
      {hint ? <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">{hint}</p> : null}
    </div>
  );
}

export function SettingsToggle({
  checked,
  disabled,
  onChange,
  label,
  hint,
}: {
  checked: boolean;
  disabled?: boolean;
  onChange: (value: boolean) => void;
  label: string;
  hint?: string;
}) {
  return (
    <label className={cn("flex gap-2.5", disabled && "opacity-60")}>
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
        className="mt-0.5"
      />
      <span className="min-w-0">
        <span className="block text-[13px] font-medium">{label}</span>
        {hint ? (
          <span className="mt-0.5 block text-[11.5px] leading-relaxed text-slate-2">{hint}</span>
        ) : null}
      </span>
    </label>
  );
}
