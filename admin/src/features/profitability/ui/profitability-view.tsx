"use client";

import { useProfitabilityQuery } from "@/entities/usage";
import { Card, CardBody, CardHeader, Eyebrow, Stat } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { errorMessage } from "@/shared/lib/error-message";
import { count, won } from "@/shared/lib/format";
import { CostRatioBar } from "@/shared/ui/cost-ratio-bar";
import { PageHeader } from "@/widgets/page-header/page-header";

/**
 * 수익성 — 어느 업체가 돈을 벌어주고 어느 업체가 손해인지.
 *
 * 저장 답변 비율을 원가율 옆에 나란히 두는 이유는 상관관계가 눈에 보이게 하기 위해서다.
 * 원가율이 높은 업체는 대부분 공통 질문이 0~2개다 (admin-console-plan.md §4.3).
 */
export function ProfitabilityView() {
  const { data, isPending, isError, error, refetch } = useProfitabilityQuery();

  if (isPending) return <LoadingState />;
  if (isError) {
    return <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />;
  }

  const { stats, content } = data;
  const warn = stats.costRatioWarnPercent;

  return (
    <>
      <PageHeader title="수익성" description="업체별 원가율과 손익" />

      <Card className="mb-4">
        <CardBody className="px-[18px] py-[15px] text-[13px] leading-relaxed text-slate">
          업체가 낸 요금 대비 우리가 쓴 모델 비용의 비율입니다. 눈금은{" "}
          <b className="font-semibold text-ink">{warn}%</b> 기준선으로, 이를 넘으면 인건비와
          서버비를 빼고 남는 게 거의 없습니다. 100%를 넘으면 쓸수록 손해입니다.
        </CardBody>
      </Card>

      <div className="mb-4 grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
        <Stat
          label="계약 정가 합계"
          value={count(stats.revenueKrw)}
          // 이 값은 요금제 정가의 합이지 받은 돈이 아니다 — 결제가 실패한 업체의 금액도 들어 있다.
          // "매출"이라 부르면 정산 화면의 수납액과 다른 숫자가 같은 이름을 갖게 된다.
          detail={`${content.length}개 업체 · 수납액은 정산에서`}
        />
        <Stat
          label="이번 달 모델 원가"
          value={count(Math.round(stats.costKrw))}
          detail={`평균 원가율 ${stats.avgCostRatioPercent}%`}
        />
        <Stat
          label="저장 답변 비중"
          value={`${stats.savedAnswerPercent}%`}
          detail="원가 없는 응답"
          tone="up"
        />
        <Stat
          label="원가 초과 업체"
          value={count(stats.costExceededCount)}
          detail={stats.costExceededCount > 0 ? "쓸수록 적자" : "없음"}
          tone={stats.costExceededCount > 0 ? "down" : "neutral"}
        />
      </div>

      <Card>
        <CardHeader title="업체별 원가율" aside={<Eyebrow>이번 달 기준</Eyebrow>} />
        {content.length === 0 ? (
          <EmptyState message="집계된 사용량이 없습니다. 답변 파이프라인이 붙으면 채워집니다" />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>업체</Th>
                <Th className="w-[100px]">요금</Th>
                <Th className="w-[100px]">모델 원가</Th>
                <Th className="w-[170px]">원가율</Th>
                <Th className="w-[104px]">저장 답변</Th>
              </tr>
            </thead>
            <tbody>
              {content.map((item) => (
                <tr key={item.tenant.id} className="hover:bg-line-2/40">
                  <Td>
                    <TitleCell title={item.tenant.name} sub={item.planName ?? "요금제 없음"} />
                  </Td>
                  <Td className="tabular text-[13px] text-slate">{won(item.billedKrw)}</Td>
                  <Td
                    className={`tabular text-[13px] ${
                      item.costRatioPercent >= 100 ? "font-semibold text-brick" : "text-slate"
                    }`}
                  >
                    {won(Math.round(item.costKrw))}
                  </Td>
                  <Td>
                    <CostRatioBar percent={item.costRatioPercent} warnThreshold={warn} />
                  </Td>
                  <Td className="tabular text-[13px] text-slate">{item.savedAnswerPercent}%</Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>

      <Card className="mt-4">
        <CardHeader title="원가가 튀는 이유" />
        <CardBody className="text-[13px] leading-[1.75] text-slate">
          원가율이 높은 업체는 대부분 <b className="font-semibold text-ink">공통 질문이 0개</b>입니다.
          자주 오는 질문까지 전부 AI로 처리하면서 비용이 쌓입니다. 가장 많이 들어온 질문 상위
          10개를 뽑아 대신 등록해 주는 것만으로 원가율이 절반 가까이 떨어지는 사례가 많았습니다.
        </CardBody>
      </Card>
    </>
  );
}
