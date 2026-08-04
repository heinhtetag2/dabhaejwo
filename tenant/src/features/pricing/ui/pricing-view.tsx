import type { PublicPlan } from "@/entities/public/plan";
import { LinkButton } from "@/shared/common/button";
import { COMPANY } from "@/shared/config/company";
import { ROUTES } from "@/shared/config/routes";
import { Notice } from "@/shared/ui/notice";

/**
 * 요금제.
 *
 * <p>가격을 페이지에 적어두지 않고 `plans` 에서 읽는다 — 운영자가 요금제를 바꿨는데
 * 소개 페이지가 옛 가격을 보여주면 그 차이가 그대로 분쟁이 된다 (tenant-public-plan.md §2.2).
 *
 * <p>Server Component 다. 목록을 서버에서 받아 props 로 넘겨받는다.
 */
export function PricingView({ plans }: { plans: PublicPlan[] }) {
  return (
    <div className="mx-auto max-w-[1080px] px-5 pt-14 pb-4">
      <h1 className="text-[30px] font-semibold tracking-[-0.03em]">요금제</h1>
      <p className="mt-3 max-w-[560px] text-[14px] leading-relaxed text-slate">
        14일 무료로 먼저 써보세요. 카드 등록이 필요 없고, 체험 기간에는 요금이 청구되지 않습니다.
      </p>

      {plans.length === 0 ? (
        <Notice tone="warn" className="mt-8">
          요금제 정보를 불러오지 못했습니다.{" "}
          <a href={`mailto:${COMPANY.contactEmail}`} className="underline">
            {COMPANY.contactEmail}
          </a>
          로 문의해 주시면 안내해 드리겠습니다.
        </Notice>
      ) : (
        <>
          <div className="mt-9 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {plans.map((plan) => (
              <PlanCard key={plan.id} plan={plan} />
            ))}
          </div>

          <section className="mt-12">
            <h2 className="mb-4 text-[18px] font-semibold tracking-[-0.02em]">한도를 넘으면</h2>
            <div className="rounded-card border border-line bg-card p-5 text-[13.5px] leading-relaxed text-slate">
              <p>
                <b className="font-semibold text-ink">
                  챗봇이 답변을 멈추고 안내 문구만 표시합니다. 초과 요금은 청구되지 않습니다.
                </b>{" "}
                한도의 80%에 닿으면 미리 알려드리므로 갑자기 멈추는 일은 없습니다.
              </p>
              <p className="mt-3">
                공통 질문으로 미리 등록해 둔 답변은 AI를 거치지 않고 그대로 나갑니다. 즉시
                표시되고 <b className="font-semibold text-ink">대화 수에도 잡히지 않습니다.</b>{" "}
                자주 오는 질문을 등록해 두시면 한도를 훨씬 덜 씁니다.
              </p>
            </div>
          </section>

          <section className="mt-10 pb-6">
            <h2 className="mb-4 text-[18px] font-semibold tracking-[-0.02em]">
              결제는 어떻게 하나요
            </h2>
            <div className="rounded-card border border-line bg-card p-5 text-[13.5px] leading-relaxed text-slate">
              {/* 없는 기능을 있는 것처럼 적지 않는다 (§2.3). 카드 결제는 아직 없다. */}
              <p>
                지금은 담당자가 직접 안내해 드립니다. 대시보드에서 요금제를 신청하시면
                1영업일 안에 연락드려 계약과 수납을 도와드립니다. 카드 자동 결제는 준비 중입니다.
              </p>
              <p className="mt-3">
                세금계산서가 필요하시면 신청할 때 함께 알려주세요.
              </p>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function PlanCard({ plan }: { plan: PublicPlan }) {
  const free = plan.monthlyFee === 0 && !plan.negotiable;

  return (
    <div className="flex flex-col rounded-card border border-line bg-card p-5">
      <h2 className="text-[16px] font-semibold">{plan.name}</h2>

      <p className="mt-2.5 flex items-baseline gap-1.5">
        {plan.negotiable ? (
          <span className="text-[20px] font-semibold tracking-[-0.02em]">협의</span>
        ) : free ? (
          <span className="text-[20px] font-semibold tracking-[-0.02em]">무료</span>
        ) : (
          <>
            <span className="tabular text-[24px] font-semibold tracking-[-0.02em]">
              {plan.monthlyFee.toLocaleString()}
            </span>
            <span className="text-[13px] text-slate-2">원 / 월</span>
          </>
        )}
      </p>

      <dl className="mt-4 space-y-1.5 border-t border-line-2 pt-4 text-[12.5px]">
        <Row label="월 대화" value={limitLabel(plan.convLimit, "건")} />
        <Row label="학습 문서" value={limitLabel(plan.docLimit, "개")} />
      </dl>

      <div className="mt-5 pt-1">
        {plan.negotiable ? (
          <a
            href={`mailto:${COMPANY.contactEmail}?subject=${encodeURIComponent(`${plan.name} 요금제 문의`)}`}
            className="inline-flex w-full items-center justify-center rounded-[7px] border border-line bg-card px-3.5 py-[7.5px] text-[13.5px] font-medium transition-colors hover:bg-line-2/60"
          >
            문의하기
          </a>
        ) : (
          <LinkButton
            href={ROUTES.signup}
            variant={free ? "accent" : "default"}
            className="w-full justify-center"
          >
            {free ? "무료로 시작하기" : "이 요금제로 시작"}
          </LinkButton>
        )}
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <dt className="text-slate">{label}</dt>
      <dd className="tabular">{value}</dd>
    </div>
  );
}

/** 사실상 무제한인 값(999,999)을 그대로 보여주면 우스워진다. */
function limitLabel(limit: number, unit: string): string {
  return limit >= 999_999 ? "제한 없음" : `${limit.toLocaleString()}${unit}`;
}
