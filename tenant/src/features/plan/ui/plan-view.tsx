"use client";

import {
  BILLING_STATUS_LABEL,
  usePlanOverviewQuery,
  type BillingStatus,
} from "@/entities/tenant/plan";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { Notice } from "@/shared/ui/notice";
import { StatusBadge, type Tone } from "@/shared/ui/status-badge";

const TONE: Record<BillingStatus, Tone> = {
  PAID: "ok",
  PENDING: "warn",
  FAILED: "error",
  REFUNDED: "idle",
};

/** 한도 알림 기준. 갑자기 챗봇이 멈추는 경험은 해지로 직결된다 (tenant-plan.md §4.9). */
const WARN_PERCENT = 80;

export function PlanView() {
  const { data, isPending, isError, refetch } = usePlanOverviewQuery();

  if (isPending) {
    return <LoadingState />;
  }
  if (isError) {
    return <ErrorState message="요금제 정보를 불러오지 못했습니다" onRetry={() => void refetch()} />;
  }

  const convPercent = percent(data.usage.convCount, data.usage.convLimit);
  const docPercent = percent(data.usage.docCount, data.usage.docLimit);

  return (
    <>
      {convPercent >= WARN_PERCENT ? (
        <Notice tone={convPercent >= 100 ? "error" : "warn"} className="mb-4">
          {convPercent >= 100
            ? "이번 달 대화 한도를 모두 썼습니다. 요금제를 올리면 챗봇이 다시 답합니다."
            : `이번 달 대화 한도의 ${convPercent}%를 썼습니다. 한도에 닿으면 챗봇이 답을 멈춥니다.`}
        </Notice>
      ) : null}

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="지금 쓰는 요금제" />
          <CardBody>
            <p className="flex items-baseline gap-2.5">
              <span className="text-[22px] font-semibold tracking-[-0.02em]">{data.plan.name}</span>
              <span className="tabular text-slate">
                월 {data.plan.monthlyFee.toLocaleString()}원
              </span>
            </p>
            <p className="mt-1 mb-4.5 text-[11.5px] text-slate-2">
              {data.nextBillingDate ? `다음 결제일 ${data.nextBillingDate}` : "결제 예정일 없음"}
            </p>

            <Meter
              label="이번 달 대화"
              used={data.usage.convCount}
              limit={data.usage.convLimit}
              percent={convPercent}
            />
            <Meter
              label="학습 문서"
              used={data.usage.docCount}
              limit={data.usage.docLimit}
              percent={docPercent}
              className="mt-4"
            />

            {data.savedAnswerPercent !== null && data.savedAnswerPercent > 0 ? (
              <Notice tone="info" className="mt-4.5">
                이번 달 대화의 <b className="font-semibold">{data.savedAnswerPercent}%</b>는 저장된
                답변으로 처리되어 한도를 쓰지 않았습니다. 공통 질문을 더 등록하면 이 비율이 올라갑니다.
              </Notice>
            ) : null}

            {/* TODO(stub): PG 미연동. 버튼을 만들어두고 눌리게 하면 눌러본 사람이 기다린다. */}
            <p className="mt-4 text-[11.5px] leading-relaxed text-slate-2">
              요금제 변경·결제 수단·세금계산서는 아직 준비 중입니다. 필요하시면 문의로
              남겨주시면 운영팀이 처리해 드립니다.
            </p>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="결제 내역" aside={<Eyebrow>{data.billingRecords.length}건</Eyebrow>} />
          {data.billingRecords.length === 0 ? (
            <CardBody className="text-[13px] text-slate-2">아직 결제 내역이 없습니다.</CardBody>
          ) : (
            <ul>
              {data.billingRecords.map((record) => (
                <li
                  key={record.id}
                  className="flex flex-wrap items-center gap-3 border-b border-line-2 px-4.5 py-3 last:border-b-0"
                >
                  <span className="tabular w-[92px] shrink-0 text-[12px] text-slate-2">
                    {record.period.slice(0, 7)}
                  </span>
                  <span className="min-w-0 flex-1 text-[13px]">{data.plan.name} 월 이용료</span>
                  <span className="tabular text-[13px]">{record.amount.toLocaleString()}원</span>
                  <StatusBadge tone={TONE[record.status]} label={BILLING_STATUS_LABEL[record.status]} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}

function Meter({
  label,
  used,
  limit,
  percent: value,
  className,
}: {
  label: string;
  used: number;
  limit: number;
  percent: number;
  className?: string;
}) {
  return (
    <div className={className}>
      <p className="mb-1.5 flex justify-between text-[12.5px]">
        <span>{label}</span>
        <span className="tabular">
          {used.toLocaleString()} / {limit.toLocaleString()}
        </span>
      </p>
      <div className="h-1.5 overflow-hidden rounded-[3px] bg-line-2">
        <span
          className={
            value >= 100 ? "block h-full bg-brick" : value >= 80 ? "block h-full bg-mark" : "block h-full bg-ink"
          }
          style={{ width: `${Math.min(100, value)}%` }}
        />
      </div>
    </div>
  );
}

function percent(used: number, limit: number): number {
  if (limit <= 0) {
    return 0;
  }
  return Math.round((used / limit) * 100);
}
