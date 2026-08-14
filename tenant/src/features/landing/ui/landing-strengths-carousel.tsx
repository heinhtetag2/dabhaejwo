"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useState } from "react";

import { cn } from "@/shared/lib/cn";
import type { Language } from "@/shared/lib/language";

import { LANDING_TEXT_V2 } from "./landing-content-v2";
import { LandingStrengthsImage } from "./landing-strengths-image";

/**
 * "핵심 강점" 캐러셀 — Figma 참조(node 65:11990)는 실제 고객 사례(회사명·인물·
 * "AI 상담 해결률" %) 캐러셀이다. 우리는 아직 고객 사례가 없어 그 숫자·인용문·로고를
 * 지어낼 수 없다(가짜 후기·통계). 대신 탭+캐러셀이라는 시각 장치만 유지하고, 큰 숫자
 * 자리에는 실제 제품 사양(600자 청크, 14일 체험 등)을 넣었다 — 없는 성과 대신
 * 있는 사실을 보여준다.
 */
export function LandingStrengthsCarousel({ language }: { language: Language }) {
  const t = LANDING_TEXT_V2[language];
  const [active, setActive] = useState(0);
  const current = t.strengths[active];

  function go(delta: number) {
    setActive((prev) => (prev + delta + t.strengths.length) % t.strengths.length);
  }

  return (
    <section className="flex flex-col items-center gap-[58px] px-5 py-[80px] sm:px-[68px]">
      <div className="flex max-w-[900px] flex-col items-center gap-4 text-center">
        <h2 className="text-[28px] leading-[1.3] font-semibold tracking-[-0.7px] text-balance break-keep text-black sm:text-[36px] lg:text-[44px] lg:leading-[62px] lg:tracking-[-0.88px]">
          {t.strengthsTitle}
        </h2>
        <p className="text-[16px] leading-[1.6] text-black sm:text-[18px] sm:leading-[28px] sm:tracking-[-0.18px]">
          {t.strengthsSubtitle}
        </p>
      </div>

      <div className="flex w-full flex-col items-center gap-5">
        <div
          role="tablist"
          aria-label={t.strengthsTitle}
          className="flex flex-wrap items-center justify-center gap-1 rounded-[999px] bg-[#f7f5f2] px-[5px] py-[6px]"
        >
          {t.strengths.map((item, index) => (
            <button
              key={item.tab}
              type="button"
              role="tab"
              aria-selected={index === active}
              onClick={() => setActive(index)}
              className={cn(
                "rounded-full px-[24px] py-[13px] text-[15px] font-semibold whitespace-nowrap transition-colors sm:px-[35px] sm:text-[16px]",
                index === active ? "bg-[#313130] text-white" : "text-[#716f6d] hover:text-ink",
              )}
            >
              {item.tab}
            </button>
          ))}
        </div>

        <div className="relative w-full">
          <div className="relative flex min-h-[420px] flex-col justify-center overflow-hidden rounded-[35px] px-[60px] py-14 sm:h-[630px] sm:pr-[80px] sm:pl-[110px]">
            <LandingStrengthsImage index={active} />

            {current.stat ? (
              <p className="relative text-[56px] leading-none font-semibold tracking-[-2px] text-mark-deep sm:text-[65px] sm:tracking-[-3.25px]">
                {current.stat}
              </p>
            ) : null}
            <h3 className="relative mt-4 max-w-[500px] text-[24px] leading-[1.25] font-semibold tracking-[-0.7px] text-white sm:text-[30px] sm:leading-[42px] sm:tracking-[-1px]">
              {current.title}
            </h3>
            <p className="relative mt-3 max-w-[440px] text-[16px] leading-[1.6] text-[#c5c5c5] sm:text-[18px] sm:leading-[28px] sm:tracking-[-0.18px]">
              {current.body}
            </p>
          </div>

          <button
            type="button"
            onClick={() => go(-1)}
            aria-label="이전"
            className="absolute top-1/2 left-3 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-white/20 text-white transition-colors hover:bg-white/30 sm:left-[22px]"
          >
            <ChevronLeft aria-hidden className="size-5" />
          </button>
          <button
            type="button"
            onClick={() => go(1)}
            aria-label="다음"
            className="absolute top-1/2 right-3 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-white/20 text-white transition-colors hover:bg-white/30 sm:right-[22px]"
          >
            <ChevronRight aria-hidden className="size-5" />
          </button>
        </div>
      </div>
    </section>
  );
}
