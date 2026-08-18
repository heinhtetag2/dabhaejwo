import type {
  InputHTMLAttributes,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 대시보드의 입력 컨트롤.
 *
 * <p>화면마다 손으로 쓴 클래스가 달라 <b>같은 줄에 놓인 입력과 버튼의 높이가 어긋났다</b> —
 * 세로 여백이 다섯 종류였다. 여기 하나로 모은다.
 *
 * <p>크기는 {@code Button} 과 <b>정확히 같은 값</b>을 쓴다. 글자 크기·세로 여백·테두리가
 * 같아야 같은 높이가 되므로, 한쪽만 고치면 다시 어긋난다. 바꿀 일이 있으면 둘 다 바꾼다.
 */
/*
 * 입력은 <b>알약 모양으로 만들지 않는다.</b> 버튼은 눌리는 것이라 둥글수록 읽기 쉽지만,
 * 입력은 글자가 왼쪽 끝에서 시작하므로 곡률이 커질수록 첫 글자가 테두리에 먹힌다.
 * revamp 원본(요금제 v2)도 카드는 14px, 입력·작은 버튼은 8px 로 나눠 쓴다.
 */
const SIZE = {
  /** 폼 본문. Button 의 md 와 같은 높이다. */
  md: "px-[11px] py-[7.5px] text-[13.5px] rounded-[9px]",
  /** 툴바·표 안. Button 의 sm 과 같은 높이다. */
  sm: "px-2.5 py-[4.5px] text-[12.5px] rounded-[7px]",
} as const;

type ControlSize = keyof typeof SIZE;

const BASE =
  "w-full border border-line bg-card text-ink transition-colors " +
  "placeholder:text-slate-2 focus:border-ink-3 focus:outline-none " +
  "disabled:cursor-not-allowed disabled:bg-paper disabled:text-slate-2";

/**
 * 컨트롤 클래스만 필요할 때. 아래 컴포넌트들이 쓰는 것과 <b>같은 값</b>이다.
 *
 * <p>기존 화면이 {@code <input>} 을 직접 쓰고 있어 클래스만 갈아끼울 수 있게 열어 둔다.
 * 새로 쓰는 코드는 {@link Input}·{@link Select}·{@link Textarea} 를 쓴다.
 */
export function controlClass(size: ControlSize = "md", extra?: string): string {
  return cn(BASE, SIZE[size], extra);
}

export function Input({
  size = "md",
  className,
  ...rest
}: Omit<InputHTMLAttributes<HTMLInputElement>, "size"> & { size?: ControlSize }) {
  return <input className={cn(BASE, SIZE[size], className)} {...rest} />;
}

export function Select({
  size = "md",
  className,
  ...rest
  // select 의 기본 size 는 "보이는 줄 수"(number)라 이름이 겹친다. 우리 것으로 덮는다.
}: Omit<SelectHTMLAttributes<HTMLSelectElement>, "size"> & { size?: ControlSize }) {
  // 브라우저 기본 화살표를 남긴다. 직접 그리면 OS 마다 어긋나고 접근성만 나빠진다.
  return <select className={cn(BASE, SIZE[size], className)} {...rest} />;
}

export function Textarea({
  size = "md",
  className,
  ...rest
}: TextareaHTMLAttributes<HTMLTextAreaElement> & { size?: ControlSize }) {
  return (
    <textarea className={cn(BASE, SIZE[size], "min-h-[76px] resize-y leading-relaxed", className)} {...rest} />
  );
}

/**
 * 라벨 + 컨트롤 한 벌.
 *
 * <p>라벨을 {@code htmlFor} 로 묶는다 — 떨어져 있으면 스크린 리더에서 무엇을 입력하는지 알 수 없다.
 * 오류가 있으면 힌트를 감춘다. 둘을 같이 보여주면 지금 해야 할 말이 흐려진다.
 */
export function FormRow({
  id,
  label,
  hint,
  error,
  className,
  children,
}: {
  id: string;
  label: string;
  hint?: React.ReactNode;
  error?: string | null;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={cn("mb-4", className)}>
      <label htmlFor={id} className="mb-1.5 block text-[12.5px] font-medium">
        {label}
      </label>
      {children}
      {error ? (
        <p role="alert" className="mt-1.5 text-[11.5px] leading-relaxed text-brick">
          {error}
        </p>
      ) : hint ? (
        <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">{hint}</p>
      ) : null}
    </div>
  );
}
