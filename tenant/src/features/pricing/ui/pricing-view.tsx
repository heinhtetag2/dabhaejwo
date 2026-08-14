import type { PublicPlan } from "@/entities/public/plan";
import { COMPANY } from "@/shared/config/company";
import { cn } from "@/shared/lib/cn";
import type { Language } from "@/shared/lib/language";
import { Notice } from "@/shared/ui/notice";
import { PublicSection } from "@/shared/ui/public-section";
import { SignupCtaLink } from "@/shared/ui/signup-cta-link";

const TEXT: Record<
  Language,
  {
    heroTitle: string;
    heroSubtitle: string;
    unreachable: string;
    contactSuffix: string;
    limitEyebrow: string;
    limitTitle: string;
    limitCard1Title: string;
    limitCard1Body: string;
    limitCard2Title: string;
    limitCard2Body: string;
    billingEyebrow: string;
    billingTitle: string;
    billingBody1: string;
    billingBody2: string;
    conv: string;
    doc: string;
    convUnit: string;
    docUnit: string;
    negotiable: string;
    free: string;
    perMonth: string;
    noLimit: string;
    contact: string;
    startFree: string;
    startPlan: string;
  }
> = {
  en: {
    heroTitle: "Pay only for what you use",
    heroSubtitle:
      "Try it free for 14 days. No card required, and you won't be charged during the trial.",
    unreachable: "Couldn't load plan information.",
    contactSuffix: "and we'll help you out.",
    limitEyebrow: "Limits",
    limitTitle: "Going over your limit doesn't raise your bill",
    limitCard1Title: "The chatbot pauses and shows a notice — that's it",
    limitCard1Body:
      "You're never charged for overages. We'll warn you at 80% of your limit, so it never stops without notice.",
    limitCard2Title: "Saved answers don't count against your limit",
    limitCard2Body:
      "Pre-written answers skip the AI entirely. They show instantly and aren't counted as conversations, so registering common questions stretches your limit much further.",
    billingEyebrow: "Billing",
    billingTitle: "For now, a person will walk you through it",
    billingBody1:
      "Request a plan from your dashboard and we'll reach out within one business day to help with the contract and payment. Automatic card billing is on the way.",
    billingBody2: "Need a tax invoice? Just mention it when you apply.",
    conv: "Conversations / mo",
    doc: "Documents",
    convUnit: "",
    docUnit: "",
    negotiable: "Custom",
    free: "Free",
    perMonth: "/ mo",
    noLimit: "Unlimited",
    contact: "Contact us",
    startFree: "Start for free",
    startPlan: "Start this plan",
  },
  ko: {
    heroTitle: "쓴 만큼만 냅니다",
    heroSubtitle:
      "14일 무료로 먼저 써보세요. 카드 등록이 필요 없고, 체험 기간에는 요금이 청구되지 않습니다.",
    unreachable: "요금제 정보를 불러오지 못했습니다.",
    contactSuffix: "로 문의해 주시면 안내해 드리겠습니다.",
    limitEyebrow: "한도",
    limitTitle: "한도를 넘어도 요금이 늘지 않습니다",
    limitCard1Title: "챗봇이 멈추고 안내 문구만 표시합니다",
    limitCard1Body:
      "초과 요금은 청구되지 않습니다. 한도의 80%에 닿으면 미리 알려드리므로 갑자기 멈추는 일은 없습니다.",
    limitCard2Title: "공통 질문은 한도를 쓰지 않습니다",
    limitCard2Body:
      "미리 등록해 둔 답변은 AI를 거치지 않고 그대로 나갑니다. 즉시 표시되고 대화 수에도 잡히지 않으므로, 자주 오는 질문을 등록해 두시면 한도를 훨씬 덜 씁니다.",
    billingEyebrow: "결제",
    billingTitle: "지금은 담당자가 직접 안내해 드립니다",
    billingBody1:
      "대시보드에서 요금제를 신청하시면 1영업일 안에 연락드려 계약과 수납을 도와드립니다. 카드 자동 결제는 준비 중입니다.",
    billingBody2: "세금계산서가 필요하시면 신청할 때 함께 알려주세요.",
    conv: "월 대화",
    doc: "학습 문서",
    convUnit: "건",
    docUnit: "개",
    negotiable: "협의",
    free: "무료",
    perMonth: "원 / 월",
    noLimit: "제한 없음",
    contact: "문의하기",
    startFree: "무료로 시작하기",
    startPlan: "이 요금제로 시작",
  },
};

/**
 * 요금제.
 *
 * <p>가격을 페이지에 적어두지 않고 `plans` 에서 읽는다 — 운영자가 요금제를 바꿨는데
 * 소개 페이지가 옛 가격을 보여주면 그 차이가 그대로 분쟁이 된다 (tenant-public-plan.md §2.2).
 *
 * <p>Server Component 다. 목록을 서버에서 받아 props 로 넘겨받는다.
 *
 * <p>언어는 `language` prop 으로 받는다(`getLanguage` 는 쿠키를 읽는 요청 시점 API 라
 * 여기서 직접 부르면 이 컴포넌트를 가져다 쓰는 `pricing-version-switch.tsx`(클라이언트)가
 * 깨진다 — 상위 Server Component 가 한 번만 읽어 내려준다).
 */
export function PricingView({ plans, language }: { plans: PublicPlan[]; language: Language }) {
  const t = TEXT[language];

  return (
    <>
      <section className="mx-auto max-w-[1080px] px-5 pt-16 pb-12 text-center sm:pt-24">
        <h1 className="text-[34px] leading-[1.3] font-bold tracking-[-0.045em] text-balance sm:text-[44px]">
          {t.heroTitle}
        </h1>
        <p className="mx-auto mt-5 max-w-[520px] text-[16px] leading-[1.75] text-slate sm:text-[17px]">
          {t.heroSubtitle}
        </p>
      </section>

      {plans.length === 0 ? (
        <div className="mx-auto max-w-[1080px] px-5 pb-24">
          <Notice tone="warn" size="md">
            {t.unreachable}{" "}
            <a href={`mailto:${COMPANY.contactEmail}`} className="underline">
              {COMPANY.contactEmail}
            </a>
            {t.contactSuffix}
          </Notice>
        </div>
      ) : (
        <>
          <div className="mx-auto max-w-[1080px] px-5 pb-8">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              {plans.map((plan) => (
                <PlanCard key={plan.id} plan={plan} t={t} />
              ))}
            </div>
          </div>

          <PublicSection tone="fill" eyebrow={t.limitEyebrow} title={t.limitTitle}>
            <div className="grid gap-4 md:grid-cols-2">
              <div className="rounded-panel bg-card p-7 sm:p-8">
                <h3 className="text-[17px] font-bold tracking-[-0.02em]">{t.limitCard1Title}</h3>
                <p className="mt-3 text-[14.5px] leading-[1.75] text-slate">{t.limitCard1Body}</p>
              </div>
              <div className="rounded-panel bg-card p-7 sm:p-8">
                <h3 className="text-[17px] font-bold tracking-[-0.02em]">{t.limitCard2Title}</h3>
                <p className="mt-3 text-[14.5px] leading-[1.75] text-slate">{t.limitCard2Body}</p>
              </div>
            </div>
          </PublicSection>

          <PublicSection
            eyebrow={t.billingEyebrow}
            title={t.billingTitle}
            description={
              /* 없는 기능을 있는 것처럼 적지 않는다 (§2.3). 카드 결제는 아직 없다. */
              <>
                <p>{t.billingBody1}</p>
                <p className="mt-4">{t.billingBody2}</p>
              </>
            }
          />
        </>
      )}
    </>
  );
}

function PlanCard({ plan, t }: { plan: PublicPlan; t: (typeof TEXT)[Language] }) {
  const free = plan.monthlyFee === 0 && !plan.negotiable;

  return (
    <div
      className={cn(
        "flex flex-col rounded-panel p-7",
        // 체험 요금제가 이 화면의 목적지다. 나머지와 같은 무게로 두면 어디를 눌러야 할지 흐려진다.
        free ? "bg-ink text-white" : "bg-fill",
      )}
    >
      <h2 className={cn("text-[16px] font-bold tracking-[-0.02em]", free && "text-mark-bright")}>
        {plan.name}
      </h2>

      <p className="mt-4 flex items-baseline gap-1.5">
        {plan.negotiable ? (
          <span className="text-[28px] font-bold tracking-[-0.035em]">{t.negotiable}</span>
        ) : free ? (
          <span className="text-[28px] font-bold tracking-[-0.035em]">{t.free}</span>
        ) : (
          <>
            <span className="tabular text-[30px] font-bold tracking-[-0.04em]">
              {plan.monthlyFee.toLocaleString()}
            </span>
            <span className="text-[14px] text-slate">{t.perMonth}</span>
          </>
        )}
      </p>

      <dl
        className={cn(
          "mt-6 space-y-2.5 border-t pt-6 text-[13.5px]",
          free ? "border-white/15" : "border-edge",
        )}
      >
        <Row label={t.conv} value={limitLabel(plan.convLimit, t.convUnit, t.noLimit)} muted={free} />
        <Row label={t.doc} value={limitLabel(plan.docLimit, t.docUnit, t.noLimit)} muted={free} />
      </dl>

      <div className="mt-auto pt-8">
        {plan.negotiable ? (
          <a
            href={`mailto:${COMPANY.contactEmail}?subject=${encodeURIComponent(`${plan.name} 요금제 문의`)}`}
            className="inline-flex w-full items-center justify-center rounded-btn border border-edge bg-card px-5 py-[14px] text-[15.5px] font-semibold transition-colors hover:bg-line-2"
          >
            {t.contact}
          </a>
        ) : (
          <SignupCtaLink
            size="lg"
            className={cn(
              "w-full justify-center",
              free
                ? "border-mark bg-mark text-white hover:brightness-105"
                : "border-edge bg-card hover:bg-line-2",
            )}
          >
            {free ? t.startFree : t.startPlan}
          </SignupCtaLink>
        )}
      </div>
    </div>
  );
}

function Row({ label, value, muted }: { label: string; value: string; muted?: boolean }) {
  return (
    <div className="flex justify-between">
      <dt className={muted ? "text-slate-2" : "text-slate"}>{label}</dt>
      <dd className="tabular font-medium">{value}</dd>
    </div>
  );
}

/** 사실상 무제한인 값(999,999)을 그대로 보여주면 우스워진다. */
function limitLabel(limit: number, unit: string, noLimit: string): string {
  return limit >= 999_999 ? noLimit : `${limit.toLocaleString()}${unit}`;
}
