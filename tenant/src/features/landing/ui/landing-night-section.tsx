import type { Language } from "@/shared/lib/language";

import { LANDING_TEXT_V2 } from "./landing-content-v2";

/**
 * 새벽에도 답한다 — Figma 참조(node 65:13512)는 "CoS는 비서 실장처럼 일합니다 · 사람이
 * 잠든 새벽에도 CoS는 일합니다"라는 AI 비서(주간 브리핑·액션 제안)를 파는 섹션이다.
 * 그 CoS 도 배경에 뜬 KPI 대시보드 스크린샷(매출 리포트·차트)도 우리에게 없다.
 *
 * <p>대신 이 섹션의 실제 요지("사람이 없어도 AI 가 계속 일한다")는 챗봇에도 정확히
 * 들어맞는다 — 방문자가 새벽 3시에 물어도 챗봇은 학습된 문서로 즉시 답한다(체험·요금제
 * 어디에도 응답 가능 시간 제한이 없다). 같은 보라 그라데이션 배경·중앙 정렬 카피·
 * 떠 있는 미리보기 카드 리듬은 유지하고, 카드 내용은 시각(새벽 3시)이 찍힌 실제
 * 위젯 대화로 바꿨다 — `landing-hero-v2.tsx`·`landing-docs-section.tsx` 와 같은 이유로
 * 실제 화면 캡처 대신 직접 그린다.
 *
 * <p>`landing-view-v2.tsx` 맨 끝, 공개 레이아웃의 `PublicFooter` 바로 위에 놓인다
 * (사용자 요청 — "푸터 위에"). 푸터는 모든 공개 페이지가 공유하므로 이 섹션 자체를
 * 푸터 컴포넌트에 넣지 않는다 — 그러면 요금제·가입·로그인에도 새어나간다.
 *
 * <p>eyebrow 크기는 참조(65:13512)의 22px 원값이 아니라 `landing-flywheel-section`
 * 이 쓰는 16→18px 스케일을 따른다 — 랜딩 v2 전체의 "섹션 상단 라벨" 타이포를
 * 하나로 통일하기 위해서다(사용자 요청 — 시스템 일관성). `landing-view-v2.tsx` 상단
 * 코멘트에 이 랜딩의 타이포 체계를 정리해 뒀다.
 */
export function LandingNightSection({ language }: { language: Language }) {
  const t = LANDING_TEXT_V2[language];

  return (
    <section className="relative overflow-hidden bg-gradient-to-b from-[#8b94e2] to-[#aab0ea] px-5 pt-[80px] pb-[110px] sm:px-[68px]">
      <div className="relative z-10 mx-auto flex max-w-[780px] flex-col items-center gap-3 text-center">
        <p className="text-[16px] leading-[28px] tracking-[-0.18px] text-white/90 sm:text-[18px]">
          {t.nightEyebrow}
        </p>
        <h2 className="max-w-[640px] text-[28px] leading-[1.3] font-semibold tracking-[-0.7px] text-balance text-white sm:text-[36px] lg:text-[44px] lg:leading-[62px] lg:tracking-[-0.88px]">
          {t.nightTitle}
        </h2>
        <p className="max-w-[420px] text-[16px] leading-[1.6] text-white/85 sm:text-[18px] sm:leading-[28px] sm:tracking-[-0.18px]">
          {t.nightBody}
        </p>
      </div>

      <div className="relative z-10 mx-auto mt-14 w-full max-w-[560px] sm:mt-16">
        <NightAnswerMockup language={language} />
      </div>

      {/* 배경의 은은한 원형 장식 — Figma 의 떠 있는 스크린샷 자리를 대신한다.
          실재하지 않는 제품 화면을 얹지 않으면서도 같은 겹겹이 뜬 느낌을 준다. */}
      <div aria-hidden className="pointer-events-none absolute inset-0 z-0">
        <div className="absolute top-[8%] left-[6%] size-[180px] rounded-full bg-white/10 blur-2xl sm:size-[260px]" />
        <div className="absolute right-[8%] bottom-[10%] size-[160px] rounded-full bg-white/10 blur-2xl sm:size-[220px]" />
      </div>
    </section>
  );
}

const NIGHT_MOCKUP_TEXT: Record<
  Language,
  { brand: string; timestamp: string; question: string; answer: string }
> = {
  en: {
    brand: "dabhaejwo AI",
    timestamp: "3:12 AM",
    question: "Are you open right now?",
    answer: "Yes — I'm here 24/7. What can I help with?",
  },
  ko: {
    brand: "답해줘 AI",
    timestamp: "오전 3:12",
    question: "지금도 문의 가능한가요?",
    answer: "네, 24시간 언제든 답변드립니다. 궁금하신 점을 말씀해 주세요.",
  },
};

function NightAnswerMockup({ language }: { language: Language }) {
  const m = NIGHT_MOCKUP_TEXT[language];

  return (
    <div className="rounded-[20px] bg-white p-6 shadow-[0px_20px_50px_0px_rgba(0,0,0,0.25)]">
      <div className="flex items-center justify-between gap-3 border-b border-line-2 pb-3">
        <div className="flex items-center gap-2">
          <span className="grid size-6 place-items-center rounded-[7px] bg-ink text-[11px] font-bold text-mark-bright">
            답
          </span>
          <p className="text-[13px] font-semibold">{m.brand}</p>
        </div>
        <span className="shrink-0 rounded-full bg-fill px-2.5 py-1 text-[11.5px] font-semibold text-slate-2">
          {m.timestamp}
        </span>
      </div>
      <div className="mt-3 ml-auto max-w-[85%] rounded-block bg-mark px-3.5 py-2.5 text-[13px] text-white">
        {m.question}
      </div>
      <div className="mt-2 max-w-[90%] rounded-block bg-fill px-3.5 py-2.5 text-[13px] leading-relaxed text-ink">
        {m.answer}
      </div>
    </div>
  );
}
