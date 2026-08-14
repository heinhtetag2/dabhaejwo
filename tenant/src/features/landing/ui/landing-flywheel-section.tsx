"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

import { cn } from "@/shared/lib/cn";
import type { Language } from "@/shared/lib/language";

import { LANDING_TEXT_V2 } from "./landing-content-v2";

/** 탭 하나가 채워지는 데 걸리는 시간이자 다음 탭으로 자동 전환되는 주기다. */
const AUTO_ADVANCE_MS = 5000;

/**
 * "쓰면 쓸수록 똑똑해지는 챗봇" — Figma 참조(node 65:12221)는 채널웍스(팀챗·AI CoS 등
 * 우리에게 없는 별도 제품군)를 파는 3탭 섹션이다. 같은 3탭 구조를 유지하되 내용은
 * 저장 답변 플라이휠(저장 답변→답변 개선→후속 질문)로 채웠다 — 전부 실재하는 기능.
 *
 * <p>미리보기 이미지는 Figma 참조(node 111:7408)의 실제 대시보드 화면 3장을 그대로
 * 내려받아 썼다(hero-banner.png 와 같은 근거 — 우리 제품 화면이라 워터마크 우려가 없다).
 * 탭 순서와 화면 내용을 대조해 매칭했다: 공통 질문 관리 화면(저장 답변) ·
 * 실패 태그가 붙은 대화 로그(답변 개선) · 위젯 관리(브랜딩) 화면(브랜딩).
 * 세 번째 탭은 애초 "후속 질문"으로 적었으나 실제 화면이 위젯 브랜딩(로고·색상·인사말)이라
 * 문구가 화면 내용과 맞지 않았다 — 탭 이름과 카피를 화면에 맞게 "브랜딩"으로 고쳤다.
 *
 * <p>탭 진행 바는 실제로 자동 재생된다(사용자 요청) — Figma 참조와 같은 재생 타이머
 * 메커니즘이다. 다 채워지면 다음 탭으로 넘어가고, 직접 탭을 클릭해도 그 탭에서
 * 타이머가 새로 시작한다.
 */
export function LandingFlywheelSection({ language }: { language: Language }) {
  const t = LANDING_TEXT_V2[language];
  const [active, setActive] = useState(0);
  const tabCount = t.flywheelTabs.length;

  useEffect(() => {
    const timer = setTimeout(() => setActive((prev) => (prev + 1) % tabCount), AUTO_ADVANCE_MS);
    return () => clearTimeout(timer);
  }, [active, tabCount]);

  return (
    <section className="px-5 py-[80px] sm:px-[68px]">
      <div className="flex w-full flex-col gap-[35px]">
        <div className="flex flex-col gap-3">
          <p className="text-[16px] leading-[28px] tracking-[-0.18px] text-black sm:text-[18px]">{t.flywheelEyebrow}</p>
          <div className="flex flex-col items-start gap-6 lg:flex-row lg:items-end lg:justify-between">
            <h2 className="max-w-[780px] text-[28px] leading-[1.3] font-semibold tracking-[-0.7px] text-balance break-keep text-black sm:text-[36px] lg:text-[44px] lg:leading-[62px] lg:tracking-[-0.88px]">
              {t.flywheelTitle}
            </h2>
            <p className="max-w-[420px] text-[16px] leading-[1.6] text-black sm:text-[18px] sm:leading-[28px] sm:tracking-[-0.18px]">
              {t.flywheelSubtitle}
            </p>
          </div>
        </div>

        <FlywheelMockup activeIndex={active} language={language} />

        <div className="flex flex-col gap-6 sm:flex-row">
          {t.flywheelTabs.map((tab, index) => (
            <button
              key={tab.label}
              type="button"
              onClick={() => setActive(index)}
              className={cn(
                "flex flex-1 flex-col items-start gap-3 rounded-block px-6 py-[18px] text-left transition-opacity",
                index === active ? "opacity-100" : "opacity-45 hover:opacity-70",
              )}
            >
              <TabProgressBar active={index === active} />
              <span className="text-[20px] leading-[32px] font-semibold tracking-[-0.5px] text-ink sm:text-[25px]">
                {tab.label}
              </span>
              <span className="text-[15px] leading-[22px] text-slate sm:text-[16px] sm:leading-[25px] sm:tracking-[-0.16px]">
                {tab.body}
              </span>
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}

/**
 * 활성 탭에서만 `AUTO_ADVANCE_MS` 동안 0→100%로 채워진다 — 자동 전환 타이머와 같은 길이.
 * JS 상태(useState/useEffect) 없이 CSS `@keyframes` 애니메이션(`globals.css`)만 쓴다 —
 * 활성화될 때마다 이 요소가 새로 마운트되므로 애니메이션이 저절로 처음부터 재생된다.
 */
function TabProgressBar({ active }: { active: boolean }) {
  return (
    <span className="h-[5px] w-full overflow-hidden rounded-full bg-line-2">
      {active ? (
        <span
          className="block h-full rounded-full bg-ink"
          style={{
            animationName: "flywheel-progress",
            animationDuration: `${AUTO_ADVANCE_MS}ms`,
            animationTimingFunction: "linear",
            animationFillMode: "forwards",
          }}
        />
      ) : null}
    </span>
  );
}

const FLYWHEEL_IMAGES: { src: string; alt: Record<Language, string> }[] = [
  {
    src: "/landing/flywheel-saved-answers.png",
    alt: { en: "Saved-answer FAQ management screen", ko: "저장 답변(공통 질문) 관리 화면" },
  },
  {
    src: "/landing/flywheel-answer-gaps.png",
    alt: { en: "Conversation log flagging missed answers", ko: "답변 실패가 표시된 대화 로그 화면" },
  },
  {
    src: "/landing/flywheel-follow-ups.png",
    alt: { en: "Widget management screen for logo, bubble color, and greeting", ko: "로고·버블 색상·첫 인사말을 설정하는 위젯 관리 화면" },
  },
];

function FlywheelMockup({ activeIndex, language }: { activeIndex: number; language: Language }) {
  const image = FLYWHEEL_IMAGES[activeIndex % FLYWHEEL_IMAGES.length];

  return (
    <div className="overflow-hidden rounded-[35px] bg-fill">
      <Image
        key={image.src}
        src={image.src}
        alt={image.alt[language]}
        width={2640}
        height={1040}
        sizes="100vw"
        className="h-[220px] w-full object-cover object-top sm:h-auto sm:object-contain"
      />
    </div>
  );
}
