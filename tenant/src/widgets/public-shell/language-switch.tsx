"use client";

import { Check, ChevronDown, Globe } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { LANGUAGE_COOKIE, type Language } from "@/shared/lib/language";
import { cn } from "@/shared/lib/cn";

const LANGUAGES: { code: Language; label: string }[] = [
  { code: "en", label: "English" },
  { code: "ko", label: "한국어" },
];

/**
 * 헤더 언어 전환.
 *
 * <p>실제로 바뀐다 — 쿠키에 써서 서버가 다음 렌더에서 읽는다(`getLanguage`).
 * 헤더 자체는 그대로 Server Component 다. 이 버튼만 클라이언트다.
 *
 * <p>범위는 헤더·푸터·요금제 화면뿐이다(사용자 결정) — 랜딩·가입 등은 아직
 * 한국어만 있다. 그 화면들은 언어를 바꿔도 한국어로 보이는데, 그건 미번역이지
 * 버그가 아니다 — 다음 범위 확장 때 채운다.
 */
export function LanguageSwitch({ language }: { language: Language }) {
  const [open, setOpen] = useState(false);
  const [pendingLanguage, setPendingLanguage] = useState<Language | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

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

  // 쿠키 쓰기는 렌더 바깥의 부수효과라 이펙트로 둔다 — 클릭 핸들러에서 직접 쓰면 안 된다.
  useEffect(() => {
    if (!pendingLanguage) return;
    document.cookie = `${LANGUAGE_COOKIE}=${pendingLanguage}; path=/; max-age=31536000; samesite=lax`;
    router.refresh();
  }, [pendingLanguage, router]);

  function selectLanguage(next: Language) {
    setOpen(false);
    if (next === language) return;
    setPendingLanguage(next);
  }

  const current = LANGUAGES.find((item) => item.code === language) ?? LANGUAGES[0];

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="listbox"
        aria-expanded={open}
        className="flex shrink-0 items-center gap-1 rounded-[10px] px-3 py-2 text-[14.5px] font-medium whitespace-nowrap text-slate transition-colors hover:bg-fill hover:text-ink"
      >
        <Globe aria-hidden className="size-4" />
        {current.label}
        <ChevronDown aria-hidden className={cn("size-3.5 transition-transform", open && "rotate-180")} />
      </button>

      {open ? (
        <div
          role="listbox"
          aria-label="언어 선택"
          className="absolute top-full right-0 mt-1.5 min-w-[160px] rounded-[10px] border border-edge bg-card p-1 shadow-lg"
        >
          {LANGUAGES.map((item) => (
            <button
              key={item.code}
              type="button"
              role="option"
              aria-selected={item.code === language}
              onClick={() => selectLanguage(item.code)}
              className={cn(
                "flex w-full items-center justify-between gap-3 rounded-[7px] px-3 py-2 text-left text-[14px] font-medium transition-colors",
                item.code === language ? "bg-fill text-ink" : "text-slate hover:bg-fill hover:text-ink",
              )}
            >
              {item.label}
              {item.code === language ? <Check aria-hidden className="size-4 text-mark-ink" /> : null}
            </button>
          ))}
          <p className="px-3 pt-2 pb-1 text-[12px] leading-[1.5] text-slate-2">
            헤더·요금제 화면만 번역돼 있습니다.
          </p>
        </div>
      ) : null}
    </div>
  );
}
