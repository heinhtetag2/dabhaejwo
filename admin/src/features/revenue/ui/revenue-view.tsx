"use client";

import { useMonthlyRevenueQuery, useRevenueSummaryQuery } from "@/entities/revenue";
import { Card, CardBody } from "@/shared/common/card";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { errorMessage } from "@/shared/lib/error-message";
import { PageHeader } from "@/widgets/page-header/page-header";

import { RevenueMonthlyTable } from "./revenue-monthly-table";
import { RevenueRecordsTable } from "./revenue-records-table";
import { RevenueSummaryCards } from "./revenue-summary-cards";

/**
 * 정산 — 실제로 오간 돈.
 *
 * <p>수익성 화면과 짝이지만 다른 질문에 답한다. 수익성은 "이 업체가 쓰는 만큼 값을 받고
 * 있나"(원가 ÷ 정가)이고, 여기는 "우리가 이번 달에 실제로 얼마를 받았나"다.
 *
 * <p>맨 위에 네 단어의 차이를 적어두는 이유는 이 화면의 숫자 대부분이 "매출"이라는 한
 * 단어로 불려 왔기 때문이다. 정가 합계를 매출로 읽으면 결제가 실패한 업체의 돈까지
 * 받은 것으로 세게 된다.
 */
export function RevenueView() {
  const summary = useRevenueSummaryQuery();
  const monthly = useMonthlyRevenueQuery(12);

  if (summary.isPending) return <LoadingState />;
  if (summary.isError) {
    return (
      <ErrorState message={errorMessage(summary.error)} onRetry={() => void summary.refetch()} />
    );
  }

  return (
    <>
      <PageHeader title="정산" description="청구하고 실제로 받은 돈" />

      <Card className="mb-4">
        <CardBody className="px-[18px] py-[15px] text-[13px] leading-relaxed text-slate">
          <b className="font-semibold text-ink">수납액</b>이 실제 매출입니다.{" "}
          <b className="font-semibold text-ink">MRR</b>은 계약상 매달 들어올 정가의 합이라
          결제가 실패한 업체의 금액도 포함합니다 — 둘은 다른 값이고 서로 대체할 수 없습니다.{" "}
          <b className="font-semibold text-ink">마진</b>은 수납액에서 모델 원가만 뺀 값입니다.
          인건비와 서버비는 시스템이 모르므로 빠져 있지 않습니다.
        </CardBody>
      </Card>

      <RevenueSummaryCards summary={summary.data} />

      {monthly.isError ? (
        <ErrorState message={errorMessage(monthly.error)} onRetry={() => void monthly.refetch()} />
      ) : (
        <RevenueMonthlyTable rows={monthly.data ?? []} />
      )}

      {/*
        월 목록은 추이 응답에서 온다 — 목록에 있는 달만 고를 수 있으므로
        청구가 존재하지 않는 달을 골라 빈 화면을 보는 일이 없다.
        아직 안 왔으면 이번 달 하나로 시작한다.
      */}
      <RevenueRecordsTable
        periods={monthly.data?.map((row) => row.period) ?? [summary.data.period]}
      />
    </>
  );
}
