"use client";

import { Check } from "lucide-react";
import { useState, type ReactNode } from "react";

import type { PublicPlan } from "@/entities/public/plan";
import { COMPANY } from "@/shared/config/company";
import { cn } from "@/shared/lib/cn";
import type { Language } from "@/shared/lib/language";
import { FaqAccordion } from "@/shared/ui/faq-accordion";
import { Notice } from "@/shared/ui/notice";
import { SignupCtaLink } from "@/shared/ui/signup-cta-link";

import { InfoTooltip } from "./pricing-info-tooltip";

const PAGE_TEXT: Record<
  Language,
  {
    unreachable: string;
    contactSuffix: string;
    heroTitle: string;
    heroSubtitle: string;
    monthly: string;
    yearly: string;
    savePercent: (n: number) => string;
    calculator: string;
    trialBlurb: string;
    trialBullet1: string;
    trialBulletConv: (n: string) => string;
    trialBulletDoc: (n: string) => string;
    free: string;
    trialPrice: string;
    priceSuffix: string;
    perMonth: string;
    negotiable: string;
    contact: string;
    startPlan: string;
  }
> = {
  en: {
    unreachable: "Couldn't load plan information.",
    contactSuffix: "and we'll help you out.",
    heroTitle: "Fair pricing for every stage of your business",
    heroSubtitle: "From small teams to your full site's scale — pick the plan that fits.",
    monthly: "Monthly",
    yearly: "Yearly",
    savePercent: (n) => `Save ${n}%`,
    calculator: "Price calculator",
    trialBlurb: "Best if you just want simple inquiries answered automatically!",
    trialBullet1: "Start instantly, no card required",
    trialBulletConv: (n) => `Up to ${n} conversations / mo`,
    trialBulletDoc: (n) => `Up to ${n} documents`,
    free: "Free",
    trialPrice: "$0",
    priceSuffix: "",
    perMonth: "/mo",
    negotiable: "Contact us",
    contact: "Contact us",
    startPlan: "Start this plan",
  },
  ko: {
    unreachable: "요금제 정보를 불러오지 못했습니다.",
    contactSuffix: "로 문의해 주시면 안내해 드리겠습니다.",
    heroTitle: "비즈니스 단계에 따른 합리적인 가격",
    heroSubtitle: "작은 팀부터 우리 사이트 규모까지, 단계에 맞는 플랜을 선택하세요.",
    monthly: "월 결제",
    yearly: "연 결제",
    savePercent: (n) => `${n}% 절약`,
    calculator: "가격 계산기",
    trialBlurb: "단순한 문의만 자동으로 응대하고 싶다면 최고의 옵션!",
    trialBullet1: "카드 등록 없이 바로 시작",
    trialBulletConv: (n) => `월 대화 ${n}건까지`,
    trialBulletDoc: (n) => `학습 문서 ${n}개까지`,
    free: "무료",
    trialPrice: "0원",
    priceSuffix: "원~",
    perMonth: "/월",
    negotiable: "가격 문의",
    contact: "문의하기",
    startPlan: "이 요금제로 시작",
  },
};

/** 연 결제 선택 시 화면에 보여줄 월 환산가 배율 — 실제 요금제가 아니라 데모 계산(사용자 확인).
 *  토글 옆 "N% 절약" 배지도 이 값에서 계산해 숫자가 어긋나지 않게 한다. */
const YEARLY_DEMO_DISCOUNT = 0.8;

/**
 * 요금제 리디자인 초안 (v2) — Figma 참조 디자인 픽셀 재현.
 *
 * 사용자 요청: "내용은 신경 쓰지 말고 이 디자인과 똑같이" — 그래서 색·타이포·간격·배지·
 * 토글·버튼 모양까지 참조(Figma node 57:2)의 원값을 그대로 옮겼다(우리 토큰이 아닌
 * 원본 hex/px). 두 곳만 예외를 뒀고 이유가 있다:
 *
 * 1. 월/연 결제 토글 — 실제로는 연 결제 요금제가 없다(백엔드 `Plan` 엔티티는 `monthlyFee`
 *    하나뿐). 그래서 전환 시 보여주는 연 결제가는 <b>화면 표시용 데모 계산</b>이다
 *    (월 요금 × 0.8, 사용자 확인 후 결정) — 실제 결제·가입 흐름은 그대로 월 결제다.
 *    "가격 계산기"는 여전히 비클릭(장식)이다 — 그런 기능이 없다.
 * 2. 비교표 행 — 참조는 채널톡 CS 툴 기준 20여 개 행(시트 수·SAML SSO·Open API…)이다.
 *    우리 `plans` 테이블에 실재하는 축은 대화/문서 한도 둘뿐이고(백엔드 확인 완료),
 *    나머지는 전 플랜에 동일하게 존재하는 실제 기능이다 — 티어별로 다른 척하지 않는다
 *    (workflow-rules: 없는 기능을 있는 것처럼 적지 않는다).
 *
 * <p>배경은 참조와 같은 지점(카드 아래)에서 흰색으로 바뀐다 — 강조 배경을 표까지
 * 끌고 내려가지 않는다(사용자 지적).
 *
 * <p>`pricing-view.tsx` 의 복제본이다. 방향이 확정되면 그 파일을 대체하고 이 파일은 지운다.
 */
export function PricingViewV2({ plans, language }: { plans: PublicPlan[]; language: Language }) {
  const t = PAGE_TEXT[language];
  const [billing, setBilling] = useState<"monthly" | "yearly">("yearly");

  if (plans.length === 0) {
    return (
      <div className="mx-auto max-w-[1160px] px-5 py-24">
        <Notice tone="warn" size="md">
          {t.unreachable}{" "}
          <a href={`mailto:${COMPANY.contactEmail}`} className="underline">
            {COMPANY.contactEmail}
          </a>
          {t.contactSuffix}
        </Notice>
      </div>
    );
  }

  const trial = plans.find((plan) => plan.monthlyFee === 0 && !plan.negotiable);
  const tiers = plans.filter((plan) => plan !== trial);

  return (
    <>
      {/* -mt 가 헤더 자리 밑으로 히어로를 끌어올린다 — 헤더가 sticky·투명이라 이 구간에서는
          헤더 뒤로 보라색이 그대로 비친다. 헤더가 이제 고정 높이가 아니라 상하 16px 패딩으로
          커지므로(`public-header.tsx`) 실제 높이보다 여유 있게 잡아 안쪽 pt 로 도로 채운다 —
          로고 옆 텍스트가 헤더와 겹치지 않게. */}
      <section className="-mt-[76px] bg-[#dbecfd]">
        <div className="mx-auto max-w-[1160px] px-5 pt-[76px] sm:pt-[100px]">
          <div className="flex flex-col gap-6 pt-4 sm:pt-6 lg:flex-row lg:items-start lg:justify-between lg:gap-10 lg:pt-8">
            <h1 className="text-[32px] leading-[1.2] font-bold tracking-[-0.03em] text-balance text-[#1c445a] sm:text-[48px] sm:leading-[1.15] lg:max-w-[560px] lg:text-[56px] lg:leading-[1.15] lg:tracking-[-2px]">
              {t.heroTitle}
            </h1>

            <div className="flex flex-col items-start gap-5 lg:w-[320px] lg:shrink-0 lg:pt-3">
              <p className="text-[15px] leading-[1.6] font-semibold text-[#1c445a] sm:text-[17px]">
                {t.heroSubtitle}
              </p>

              {/* 월/연 결제 토글 — 실제로 눌린다. 연 결제 요금제가 백엔드에 없어
                  아래 표시가는 화면 계산(월 요금 × YEARLY_DEMO_DISCOUNT)이다 — 가입·결제는
                  그대로 월 결제만 있다. 배지 숫자도 같은 상수에서 계산해 표시가와 어긋나지 않는다.
                  참조의 노란 배지 대신 이 페이지에 이미 쓰인 "추천" 배지와 같은 보라 그라데이션을 쓴다. */}
              <div
                role="radiogroup"
                aria-label={`${t.yearly} / ${t.monthly}`}
                className="mb-6 inline-flex select-none items-center gap-1 rounded-full border border-[#1c445a]/25 bg-transparent p-1"
              >
                <div className="relative">
                  <button
                    type="button"
                    role="radio"
                    aria-checked={billing === "yearly"}
                    onClick={() => setBilling("yearly")}
                    className={cn(
                      "rounded-full px-7 py-2.5 text-[14px] font-semibold whitespace-nowrap transition-colors",
                      billing === "yearly" ? "bg-[#242428] text-white" : "text-[#1c445a]/50 hover:text-[#1c445a]/75",
                    )}
                  >
                    {t.yearly}
                  </button>

                  <span className="pointer-events-none absolute -bottom-3.5 left-1/2 -translate-x-1/2 rounded-[7px] bg-gradient-to-r from-[#2c5f8a] to-[#4f8fc0] px-2.5 py-0.5 text-[12px] font-bold whitespace-nowrap tracking-[-0.01em] text-white">
                    {t.savePercent(Math.round((1 - YEARLY_DEMO_DISCOUNT) * 100))}
                  </span>
                </div>
                <button
                  type="button"
                  role="radio"
                  aria-checked={billing === "monthly"}
                  onClick={() => setBilling("monthly")}
                  className={cn(
                    "rounded-full px-7 py-2.5 text-[14px] font-semibold whitespace-nowrap transition-colors",
                    billing === "monthly" ? "bg-[#242428] text-white" : "text-[#1c445a]/50 hover:text-[#1c445a]/75",
                  )}
                >
                  {t.monthly}
                </button>
              </div>
            </div>
          </div>

          {trial ? (
            <div className="mt-10 flex flex-col gap-5 rounded-[14px] bg-white p-6 sm:mt-12 sm:flex-row sm:items-center sm:justify-between">
              <div className="shrink-0 sm:w-[210px]">
                <p className="text-[17px] font-semibold tracking-[-0.02em] text-black/85">{trial.name}</p>
                <p className="mt-3 flex items-baseline gap-1">
                  <span className="text-[24px] font-semibold tracking-[-0.02em] text-black/85">
                    {t.free}
                  </span>
                  <span className="text-[14px] text-black/40">{t.trialPrice}</span>
                </p>
              </div>

              <div className="flex flex-col gap-2 sm:flex-1 sm:pl-10">
                <p className="text-[15px] tracking-[-0.01em] text-black/85">{t.trialBlurb}</p>
                <TrialItem>{t.trialBullet1}</TrialItem>
                <TrialItem>{t.trialBulletConv(trial.convLimit.toLocaleString())}</TrialItem>
                <TrialItem>{t.trialBulletDoc(trial.docLimit.toLocaleString())}</TrialItem>
              </div>
            </div>
          ) : null}

          <div className="mt-4 overflow-hidden rounded-t-[14px] border border-b-0 border-black/[0.08] bg-white">
            <div className="grid sm:grid-cols-[289px_repeat(3,1fr)]">
              <div className="hidden sm:block" />
              {tiers.map((plan) => (
                <TierCard key={plan.id} plan={plan} t={t} language={language} billing={billing} />
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* 참조(image 2)와 같은 지점에서 배경이 흰색으로 바뀐다 — 강조 배경은 히어로+카드까지만 쓴다.
          카드 박스와 표 박스 사이에 여백을 두지 않고 위 테두리 하나로만 이어붙여 한 표처럼 보이게 한다. */}
      <section className="bg-white">
        <div className="mx-auto max-w-[1160px] px-5 pb-16 sm:pb-20">
          <div className="hidden overflow-hidden rounded-b-[14px] border border-black/[0.08] bg-white sm:block">
            <LimitsTable plans={tiers} language={language} />
          </div>
        </div>
      </section>

      <FaqAccordion
        heading={FAQ_HEADING[language]}
        faqs={PRICING_FAQS[language]}
        className="bg-white pb-16 sm:pb-24"
      />
    </>
  );
}

function TrialItem({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-center gap-2.5 text-[15px] tracking-[-0.01em] text-black/85">
      <Check aria-hidden className="size-5 shrink-0 text-black/40" />
      {children}
    </div>
  );
}

const TIER_COPY: Record<string, Record<Language, string>> = {
  STARTER: {
    en: "Just starting to add a chatbot to a small site",
    ko: "이제 막 챗봇을 붙이는 소규모 사이트",
  },
  BUSINESS: {
    en: "A growing company fielding a lot of inquiries",
    ko: "문의가 몰리는 성장기 업체",
  },
  ENTERPRISE: {
    en: "A large organization that needs a custom contract",
    ko: "맞춤 계약이 필요한 대규모 조직",
  },
};
const RECOMMENDED_CODE = "BUSINESS";
const RECOMMENDED_TEXT: Record<Language, string> = { en: "Recommended", ko: "추천" };

function TierCard({
  plan,
  t,
  language,
  billing,
}: {
  plan: PublicPlan;
  t: (typeof PAGE_TEXT)[Language];
  language: Language;
  billing: "monthly" | "yearly";
}) {
  const blurb = TIER_COPY[plan.code]?.[language];
  const recommended = plan.code === RECOMMENDED_CODE;
  const recommendedLabel = RECOMMENDED_TEXT[language];
  const displayedFee =
    billing === "yearly" ? Math.round(plan.monthlyFee * YEARLY_DEMO_DISCOUNT) : plan.monthlyFee;

  return (
    <div className="flex flex-col border-t border-black/[0.08] p-5 sm:border-t-0 sm:border-l">
      <div className="flex h-11 items-start">
        <div className="flex items-center gap-2.5">
          <h2 className="text-[24px] font-semibold tracking-[-0.02em] text-black/85">
            {plan.name}
          </h2>
          {recommended ? (
            <span className="rounded-full bg-gradient-to-r from-[#2c5f8a] to-[#4f8fc0] px-2.5 py-0.5 text-[13px] font-semibold tracking-[-0.02em] text-white">
              {recommendedLabel}
            </span>
          ) : null}
        </div>
      </div>

      {blurb ? <p className="mt-1 text-[15px] tracking-[-0.01em] text-black/40">{blurb}</p> : null}

      <div className="mt-5 flex flex-1 flex-col justify-end gap-5">
        <p className="flex items-baseline gap-1">
          {plan.negotiable ? (
            <span className="text-[24px] font-semibold tracking-[-0.02em] text-black/85">
              {t.negotiable}
            </span>
          ) : (
            <>
              <span className="tabular text-[24px] font-semibold tracking-[-0.02em] text-black/85">
                {displayedFee.toLocaleString()}
                {t.priceSuffix}
              </span>
              <span className="text-[14px] text-black/85">{t.perMonth}</span>
            </>
          )}
        </p>

        {plan.negotiable ? (
          <a
            href={`mailto:${COMPANY.contactEmail}?subject=${encodeURIComponent(`${plan.name} 요금제 문의`)}`}
            className="inline-flex h-9 w-full items-center justify-center rounded-[8px] border border-black/[0.16] px-2.5 text-[14px] font-bold text-black/85 transition-colors hover:bg-black/[0.04]"
          >
            {t.contact}
          </a>
        ) : (
          <SignupCtaLink className="inline-flex h-9 w-full items-center justify-center rounded-[8px] bg-[#242428] px-2.5 text-[14px] font-bold text-white transition-colors hover:bg-black">
            {t.startPlan}
          </SignupCtaLink>
        )}
      </div>
    </div>
  );
}

const LIMITS_TEXT: Record<
  Language,
  {
    convLabel: string;
    convHint: string;
    docLabel: string;
    docHint: string;
    negotiable: string;
    noLimit: string;
    included: string;
    notIncluded: string;
  }
> = {
  en: {
    convLabel: "Conversations / mo",
    convHint: "How many conversations the chatbot actually answered this month, excluding saved answers.",
    docLabel: "Documents",
    docHint: "How many documents the chatbot can learn from.",
    negotiable: "Custom",
    noLimit: "Unlimited",
    included: "Included",
    notIncluded: "Not included",
  },
  ko: {
    convLabel: "월 대화 한도",
    convHint: "저장 답변을 제외하고, 이번 달 챗봇이 실제로 답한 대화 수입니다.",
    docLabel: "학습 문서 한도",
    docHint: "챗봇이 학습할 수 있는 문서 개수입니다.",
    negotiable: "협의",
    noLimit: "제한 없음",
    included: "포함",
    notIncluded: "미포함",
  },
};

/**
 * TODO(mock): 지금 이 프로젝트에서 전 플랜에 실제로 동일하게 제공되는 기능이다
 * (`CLAUDE.md` M1~M9, 이 페이지 FAQ "플랜 간 차이는 무엇인가요?" 항목 — 기능이 아니라
 * 대화·문서 한도만 다르다). 아래 `ROW_TIER_ACCESS` 로 STARTER/BUSINESS/ENTERPRISE 별
 * 체크 여부를 가른 것은 **사용자가 명시적으로 요청한 시각적 목업**이지 실제 기능 게이팅이
 * 아니다 — 백엔드에 티어별 기능 플래그가 없다. 이 표가 최종 디자인으로 확정되면 실제
 * 게이팅을 만들거나(백엔드 결정 필요), 이 표를 지금처럼 전부 체크로 되돌려야 한다.
 * `docs/IMPROVEMENTS.md` P0 참조.
 */
const INCLUDED_ROWS: Record<Language, { label: string; hint: string }[]> = {
  en: [
    {
      label: "Saved answers (common questions)",
      hint: "Pre-written answers skip the AI entirely, so they don't count as conversations.",
    },
    { label: "Follow-up question suggestions", hint: "Shows related questions the company picked, under each answer." },
    { label: "Widget branding (logo, colors)", hint: "Match the chatbot launcher and panel colors to your brand." },
    { label: "Conversation logs & contact capture", hint: "See visitor conversation history and any contact info they left, from the dashboard." },
    { label: "Invite teammates", hint: "Invite colleagues to manage the dashboard together." },
    { label: "Real-time notifications", hint: "Get notified in the console for events like signups, inquiries, or hitting a limit." },
    { label: "Widget on/off control", hint: "Turn the chatbot on or off instantly if training is incomplete or an answer looks wrong." },
    { label: "Allowed site addresses", hint: "Register or remove the site addresses the chatbot can appear on, and check whether real calls came in." },
    { label: "Auto-billing (card on file)", hint: "Register a card once and get billed automatically on the same date each month." },
    { label: "Persona & handoff settings", hint: "Set the chatbot's tone, what it says when unsure, and when to hand off to a human." },
    { label: "Document upload & retraining", hint: "Upload files to train the chatbot, and retrain just the ones that failed." },
    { label: "Answer gap review", hint: "See questions the chatbot missed or got a thumbs-down on — turn one into an answer and it becomes a saved answer." },
    { label: "Two-factor login (OTP)", hint: "After your password, a 6-digit email code is required to sign in." },
  ],
  ko: [
    {
      label: "저장 답변(공통 질문)",
      hint: "미리 등록해 둔 답변은 AI를 거치지 않고 그대로 나가 대화 수에 잡히지 않습니다.",
    },
    { label: "후속 질문 추천", hint: "답변 아래에 업체가 지정한 관련 질문을 함께 보여줍니다." },
    { label: "위젯 브랜딩(로고·색상)", hint: "챗봇 런처와 패널 색상을 브랜드에 맞게 바꿀 수 있습니다." },
    { label: "대화 로그·연락처 수집", hint: "방문자와의 대화 기록과 남긴 연락처를 대시보드에서 확인할 수 있습니다." },
    { label: "팀원 초대", hint: "동료를 초대해 함께 대시보드를 관리할 수 있습니다." },
    { label: "실시간 알림", hint: "가입·문의·한도 초과 같은 이벤트를 콘솔에서 바로 알려드립니다." },
    { label: "위젯 노출 제어", hint: "학습이 덜 됐거나 답변이 이상할 때 챗봇을 바로 껐다 켤 수 있습니다." },
    { label: "허용 주소 관리", hint: "챗봇을 띄울 홈페이지 주소를 직접 등록·삭제하고 실제 호출 여부를 확인할 수 있습니다." },
    { label: "자동 결제(카드 등록)", hint: "카드를 한 번 등록하면 매달 같은 날짜에 자동으로 결제됩니다." },
    { label: "말투·상담 설정", hint: "챗봇의 성격과 모를 때 할 말, 상담원 연결 조건을 직접 정할 수 있습니다." },
    { label: "지식 문서 업로드·재학습", hint: "파일을 올려 챗봇을 학습시키고, 실패한 문서만 골라 다시 학습시킬 수 있습니다." },
    { label: "답변 개선", hint: "챗봇이 못 답했거나 👎 받은 질문을 모아 보여주고, 답을 등록하면 바로 공통 질문이 됩니다." },
    { label: "로그인 2단계 인증", hint: "비밀번호 입력 후 메일로 받은 6자리 코드를 맞혀야 로그인됩니다." },
  ],
};

/**
 * 목업 전용 티어 게이팅 — `INCLUDED_ROWS`(en/ko)와 같은 순서·같은 길이로 정렬된다.
 * 실제 권한 체크가 아니라 화면 시안일 뿐이다(위 TODO(mock) 참조).
 */
const ROW_TIER_ACCESS: string[][] = [
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 저장 답변(공통 질문)
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 후속 질문 추천
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 위젯 브랜딩(로고·색상)
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 대화 로그·연락처 수집
  ["BUSINESS", "ENTERPRISE"], // 팀원 초대
  ["BUSINESS", "ENTERPRISE"], // 실시간 알림
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 위젯 노출 제어
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 허용 주소 관리
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 자동 결제(카드 등록)
  ["BUSINESS", "ENTERPRISE"], // 말투·상담 설정
  ["STARTER", "BUSINESS", "ENTERPRISE"], // 지식 문서 업로드·재학습
  ["ENTERPRISE"], // 답변 개선
  ["BUSINESS", "ENTERPRISE"], // 로그인 2단계 인증
];

/**
 * 참조의 20여 개 B2B 기능 비교표 자리. 실재하는 축(대화·문서 한도)은 데이터로,
 * 나머지 행의 체크 여부는 `ROW_TIER_ACCESS` 목업이다(위 TODO(mock) 참조) —
 * 참조와 같은 행 스타일·구분선으로 재현했다.
 */
function LimitsTable({ plans, language }: { plans: PublicPlan[]; language: Language }) {
  const t = LIMITS_TEXT[language];

  return (
    <div className="divide-y divide-black/[0.08]">
      <TableRow label={t.convLabel} hint={t.convHint}>
        {plans.map((plan) => (
          <TableCell key={plan.id}>
            {plan.negotiable ? t.negotiable : limitLabel(plan.convLimit, t.noLimit)}
          </TableCell>
        ))}
      </TableRow>
      <TableRow label={t.docLabel} hint={t.docHint}>
        {plans.map((plan) => (
          <TableCell key={plan.id}>
            {plan.negotiable ? t.negotiable : limitLabel(plan.docLimit, t.noLimit)}
          </TableCell>
        ))}
      </TableRow>
      {INCLUDED_ROWS[language].map((row, index) => {
        const allowedCodes = ROW_TIER_ACCESS[index] ?? [];
        return (
          <TableRow key={row.label} label={row.label} hint={row.hint}>
            {plans.map((plan) => {
              const included = allowedCodes.includes(plan.code);
              return (
                <TableCell key={plan.id}>
                  {included ? (
                    <>
                      <Check aria-hidden className="size-5 text-black/70" />
                      <span className="sr-only">{t.included}</span>
                    </>
                  ) : (
                    <>
                      <span aria-hidden className="text-[15px] text-black/20">
                        –
                      </span>
                      <span className="sr-only">{t.notIncluded}</span>
                    </>
                  )}
                </TableCell>
              );
            })}
          </TableRow>
        );
      })}
    </div>
  );
}

function TableRow({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <div className="grid grid-cols-[289px_repeat(3,1fr)]">
      <div className="flex items-center gap-1.5 px-5 py-3">
        <span className="text-[15px] tracking-[-0.02em] text-black/85">{label}</span>
        {hint ? <InfoTooltip text={hint} /> : null}
      </div>
      {children}
    </div>
  );
}

function TableCell({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-center justify-center border-l border-black/[0.08] px-4 py-3">
      <span className="tabular text-[15px] tracking-[-0.02em] text-black/85">{children}</span>
    </div>
  );
}

function limitLabel(limit: number, noLimit: string): string {
  return limit >= 999_999 ? noLimit : limit.toLocaleString();
}

const PRICING_FAQS: Record<Language, { q: string; a: string }[]> = {
  en: [
    {
      q: "Will I be charged automatically once the trial ends?",
      a: "No. If you haven't registered a card, there's no way to charge you. If you'd like to keep using it, request a plan from your dashboard.",
    },
    {
      q: "What's different between plans?",
      a: "The monthly conversation limit and document limit. Every feature is the same across all plans.",
    },
    {
      q: "What happens if I go over my limit?",
      a: "The chatbot pauses and shows a notice — you're never charged for overages. We'll warn you at 80% of your limit.",
    },
    {
      q: "Do saved answers count against the conversation limit?",
      a: "No. Pre-written answers skip the AI entirely, so they aren't counted as conversations.",
    },
    {
      q: "How does billing work?",
      a: "Request a plan from your dashboard and a person will reach out within one business day to help with the contract and payment. Automatic card billing is on the way.",
    },
    {
      q: "Can I change or cancel my plan?",
      a: "Yes, any time, from your dashboard. After cancelling, we keep a 30-day grace period before deleting your training data.",
    },
  ],
  ko: [
    {
      q: "체험이 끝나면 자동으로 결제되나요?",
      a: "아닙니다. 카드를 등록하지 않았다면 결제될 방법이 없습니다. 체험을 계속 쓰고 싶으시면 대시보드에서 요금제를 신청해 주세요.",
    },
    {
      q: "플랜 간 차이는 무엇인가요?",
      a: "월 대화 한도와 학습 문서 한도입니다. 기능은 모든 플랜에서 동일하게 제공됩니다.",
    },
    {
      q: "한도를 넘으면 어떻게 되나요?",
      a: "챗봇이 멈추고 안내 문구만 표시됩니다. 초과 요금은 청구되지 않습니다. 한도의 80%에 닿으면 미리 알려드립니다.",
    },
    {
      q: "공통 질문도 대화 한도에 들어가나요?",
      a: "들어가지 않습니다. 미리 등록해 둔 답변은 AI를 거치지 않고 그대로 나가므로 대화 수에 잡히지 않습니다.",
    },
    {
      q: "결제는 어떻게 이루어지나요?",
      a: "대시보드에서 요금제를 신청하시면 1영업일 안에 담당자가 연락드려 계약과 수납을 도와드립니다. 카드 자동 결제는 준비 중입니다.",
    },
    {
      q: "플랜을 바꾸거나 해지할 수 있나요?",
      a: "언제든 대시보드에서 변경·해지를 요청할 수 있습니다. 해지 후에는 30일 유예 기간을 두고 학습 데이터를 삭제합니다.",
    },
  ],
};

const FAQ_HEADING: Record<Language, string> = { en: "Frequently asked questions", ko: "자주 묻는 질문" };
