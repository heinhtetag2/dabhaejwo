"use client";

import { useState } from "react";

import {
  BILLING_STATUS_LABEL,
  usePlanChange,
  usePlanOverviewQuery,
  type BillingStatus,
} from "@/entities/tenant/plan";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { env } from "@/shared/config/env";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { Notice } from "@/shared/ui/notice";
import { StatusBadge, type Tone } from "@/shared/ui/status-badge";
import { controlClass } from "@/shared/common/control";
import { BillingMethodCard } from "./billing-method-card";
import { BillingReturn } from "./billing-return";
import { useBillingMethodQuery } from "@/entities/tenant/billing";

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
      <BillingReturn />

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

            <PlanChange currentPlanCode={data.plan.id} currentIsFree={data.plan.monthlyFee <= 0} />

            <p className="mt-3 text-[11.5px] leading-relaxed text-slate-2">
              요금제를 바꾸면 등록된 카드로 바로 결제되고, 이후 매달 같은 날짜에 자동으로
              결제됩니다. 세금계산서가 필요하시면 문의로 남겨 주세요.
            </p>
          </CardBody>
        </Card>

        <BillingMethodCard />

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

/**
 * 유료 전환 신청.
 *
 * <p>요금제 목록은 <b>공개 엔드포인트</b>에서 읽는다 — 소개 페이지와 같은 목록을 보여줘야
 * "봤던 요금제가 여기 없다"는 일이 안 생긴다.
 */
function PlanChange({
  currentPlanCode,
  currentIsFree,
}: {
  currentPlanCode: string;
  currentIsFree: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [plans, setPlans] = useState<PublicPlanOption[] | null>(null);
  const [planCode, setPlanCode] = useState("");
  const change = usePlanChange();
  const method = useBillingMethodQuery();

  const load = () => {
    setOpen(true);
    if (plans !== null) {
      return;
    }
    void fetch(new URL("/api/public/plans", env.apiBaseUrl))
      .then((response) => (response.ok ? response.json() : []))
      .then((rows: PublicPlanOption[]) => {
        // 협의 요금제는 금액이 없어 자동 결제할 수 없다. 체험은 되돌아갈 수 없다.
        const sellable = rows.filter((row) => !row.negotiable && row.code !== "TRIAL");
        setPlans(sellable);
        setPlanCode(sellable[0]?.code ?? "");
      })
      .catch(() => setPlans([]));
  };

  if (change.isSuccess) {
    return (
      <Notice tone="info" className="mt-4.5">
        {change.data.charged
          ? `${change.data.planName} 요금제로 바꾸고 ${change.data.amountKrw.toLocaleString()}원이 결제되었습니다.`
          : `${change.data.planName} 요금제로 바꿨습니다. 새 금액은 다음 청구일부터 적용됩니다.`}
        {change.data.receiptUrl ? (
          <>
            {" "}
            <a
              href={change.data.receiptUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="underline"
            >
              영수증 보기
            </a>
          </>
        ) : null}
      </Notice>
    );
  }

  const cardMissing = method.data?.registered === false;

  if (!open) {
    return (
      <Button variant="accent" size="sm" className="mt-4.5" onClick={load}>
        요금제 변경
      </Button>
    );
  }

  const selected = (plans ?? []).find((plan) => plan.code === planCode);

  return (
    <div className="mt-4.5 rounded-[7px] border border-line bg-paper p-3.5">
      <label htmlFor="plan-change" className="mb-1.5 block text-[12.5px] font-medium">
        바꿀 요금제
      </label>
      <select
        id="plan-change"
        value={planCode}
        onChange={(event) => setPlanCode(event.target.value)}
        className={controlClass("sm")}
      >
        {(plans ?? []).map((plan) => (
          <option key={plan.code} value={plan.code}>
            {plan.name} · 월 {plan.monthlyFee.toLocaleString()}원
          </option>
        ))}
      </select>

      {/* 누르기 전에 얼마가 나가는지 알려준다. 눌렀더니 결제됐다는 상황을 만들지 않는다. */}
      {selected ? (
        <p className="mt-2.5 text-[12px] leading-relaxed text-slate">
          {currentIsFree
            ? `지금 ${selected.monthlyFee.toLocaleString()}원이 결제되고, 매달 오늘 날짜에 자동으로 결제됩니다.`
            : "이번 달은 이미 결제되어 추가로 청구되지 않습니다. 새 금액은 다음 청구일부터 적용됩니다."}
        </p>
      ) : null}

      {cardMissing ? (
        <Notice tone="warn" className="mt-3">
          카드를 먼저 등록해 주세요. 오른쪽 결제수단에서 등록할 수 있습니다.
        </Notice>
      ) : null}

      {change.isError ? (
        <Notice tone="error" className="mt-3">
          {change.error instanceof ApiError ? change.error.message : "요금제를 바꾸지 못했습니다"}
        </Notice>
      ) : null}

      <div className="mt-3 flex gap-2">
        <Button
          variant="accent"
          size="sm"
          disabled={planCode.length === 0 || cardMissing || change.isPending}
          onClick={() => change.mutate(planCode)}
        >
          {change.isPending
            ? "결제하는 중…"
            : currentIsFree
              ? "결제하고 바꾸기"
              : "요금제 바꾸기"}
        </Button>
        <Button size="sm" onClick={() => setOpen(false)}>
          취소
        </Button>
      </div>
      <p className="mt-2 text-[11px] text-slate-2">
        협의가 필요한 기업 요금제는 문의로 남겨 주세요. (현재 요금제 id{" "}
        {currentPlanCode.slice(0, 8)})
      </p>
    </div>
  );
}

interface PublicPlanOption {
  code: string;
  name: string;
  monthlyFee: number;
  negotiable: boolean;
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
