import type { RevenueSummary } from "@/entities/revenue";
import { Card, CardBody, Stat } from "@/shared/common/card";
import { count, won } from "@/shared/lib/format";

/**
 * 이번 달 지표 4종.
 *
 * 카드 순서가 곧 질문 순서다 — "얼마 받았나 → 못 받은 건 얼마인가 → 앞으로 얼마 들어오나
 * → 그래서 남는 건 얼마인가". MRR 을 맨 앞에 두면 계약상의 숫자가 실제 수납보다
 * 커 보이는 자리를 차지한다.
 */
export function RevenueSummaryCards({ summary }: { summary: RevenueSummary }) {
  const hasOutstanding = summary.outstandingKrw > 0;

  return (
    <>
      <div className="mb-4 grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
        <Stat
          label="이번 달 수납액"
          value={count(summary.collectedKrw)}
          detail={`결제 완료 ${summary.paidCount}건 · 실제로 받은 돈`}
          tone="up"
        />
        <Stat
          label="미수금"
          value={count(summary.outstandingKrw)}
          detail={
            hasOutstanding ? `${summary.unpaidCount}건 미결제 — 쫓아가야 할 돈` : "밀린 청구 없음"
          }
          tone={hasOutstanding ? "down" : "neutral"}
        />
        <Stat
          label="MRR"
          value={count(summary.mrrKrw)}
          // 계약 기준이라 수납액과 다르다. 같은 화면에 나란히 두면서 다른 값이면
          // "둘 중 뭐가 맞나"가 되므로 무엇인지 카드가 직접 밝힌다.
          detail="계약 기준 정가 — 수납액과 다르다"
        />
        <Stat
          label="마진"
          value={count(Math.round(summary.marginKrw))}
          detail={
            summary.marginPercent === null
              ? "받은 돈이 없어 비율 없음"
              : `${summary.marginPercent}% · 모델 원가 ${won(Math.round(summary.modelCostKrw))} 뺀 값`
          }
          tone={summary.marginKrw < 0 ? "down" : "neutral"}
        />
      </div>

      {summary.trialTenantCount > 0 ? (
        <Card className="mb-4">
          <CardBody className="px-[18px] py-[15px] text-[13px] leading-relaxed text-slate">
            체험 중인 업체 <b className="font-semibold text-ink">{summary.trialTenantCount}곳</b>이
            이번 달 <b className="font-semibold text-ink">{won(Math.round(summary.trialCostKrw))}</b>
            의 모델 원가를 썼습니다. 받는 돈이 없으므로 <b className="font-semibold text-ink">
              전액이 손실
            </b>
            입니다. 수익성 화면의 원가율에는 이 금액이 잡히지 않습니다 — 정가가 0원이라 나눌 수
            없어 <span className="font-mono text-[12px]">0% / 정상</span>으로 표시됩니다.
          </CardBody>
        </Card>
      ) : null}
    </>
  );
}
