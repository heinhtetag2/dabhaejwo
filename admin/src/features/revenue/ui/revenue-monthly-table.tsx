import type { MonthlyRevenue } from "@/entities/revenue";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState } from "@/shared/common/states";
import { Table, Td, Th } from "@/shared/common/table";
import { cn } from "@/shared/lib/cn";
import { count } from "@/shared/lib/format";

/**
 * 월별 추이.
 *
 * 청구·수납을 나란히 두는 이유는 둘이 벌어지는 달이 곧 미수가 쌓인 달이기 때문이다.
 * 한 열로 합치면 그 사실이 보이지 않는다.
 */
export function RevenueMonthlyTable({ rows }: { rows: MonthlyRevenue[] }) {
  return (
    <Card className="mb-4">
      <CardHeader title="월별 추이" aside={<Eyebrow>최신이 위</Eyebrow>} />
      {rows.length === 0 ? (
        <EmptyState message="집계된 청구가 없습니다" />
      ) : (
        <>
          <Table>
            <thead>
              <tr>
                <Th className="w-[86px]">청구월</Th>
                <Th className="w-[104px]">청구</Th>
                <Th className="w-[104px]">수납</Th>
                <Th className="w-[92px]">환불</Th>
                <Th className="w-[104px]">모델 원가</Th>
                <Th className="w-[110px]">마진</Th>
                <Th className="w-[72px]">가입</Th>
                <Th className="w-[124px]">유료 전환</Th>
                <Th className="w-[64px]">해지</Th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.period} className="hover:bg-line-2/40">
                  <Td className="tabular font-medium">{row.period}</Td>
                  <Td className="tabular text-[13px] text-slate">{count(row.billedKrw)}</Td>
                  <Td className="tabular text-[13px] font-medium">{count(row.collectedKrw)}</Td>
                  <Td
                    className={cn(
                      "tabular text-[13px]",
                      row.refundedKrw > 0 ? "text-brick" : "text-slate-2",
                    )}
                  >
                    {count(row.refundedKrw)}
                  </Td>
                  <Td className="tabular text-[13px] text-slate">
                    {count(Math.round(row.modelCostKrw))}
                  </Td>
                  <Td
                    className={cn(
                      "tabular text-[13px]",
                      row.marginKrw < 0 ? "font-semibold text-brick" : "text-slate",
                    )}
                  >
                    {count(Math.round(row.marginKrw))}
                  </Td>
                  <Td className="tabular text-[13px] text-slate">{count(row.signupCount)}</Td>
                  <Td className="tabular text-[13px]">
                    {row.conversionPercent === null ? (
                      // 가입이 없던 달. 0% 로 두면 "아무도 전환 안 했다"로 읽힌다.
                      <span className="text-slate-2">—</span>
                    ) : (
                      <>
                        <span className="font-medium">{row.conversionPercent}%</span>
                        <span className="ml-1 text-[11.5px] text-slate-2">
                          {row.convertedCount}/{row.signupCount}
                        </span>
                        {/*
                          말일 가입자의 체험이 안 끝난 달이다. 안 밝히면 운영자가
                          "이번 달 전환율이 급락했다"고 잘못 읽는다.
                        */}
                        {row.cohortOpen ? (
                          <span className="ml-1.5 font-mono text-[10.5px] text-slate-2">집계 중</span>
                        ) : null}
                      </>
                    )}
                  </Td>
                  <Td className="tabular text-[13px] text-slate">{count(row.churnedCount)}</Td>
                </tr>
              ))}
            </tbody>
          </Table>
          <CardBody className="border-t border-line-2 pt-3 text-[12px] leading-relaxed text-slate-2">
            전환율은 <b className="font-medium text-slate">가입 코호트</b> 기준입니다 — 그 달에
            가입한 업체 중 지금까지 한 번이라도 결제한 곳의 비율입니다. 체험이 14일이라 가입월과
            첫 결제월은 대부분 다릅니다. <b className="font-medium text-slate">집계 중</b>인 달은
            말일 가입자의 체험이 끝나지 않아 숫자가 더 오를 수 있습니다.
          </CardBody>
        </>
      )}
    </Card>
  );
}
