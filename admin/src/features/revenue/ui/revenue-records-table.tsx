"use client";

import Link from "next/link";
import { useState } from "react";

import { useBillingRecordsQuery, type BillingStatus } from "@/entities/revenue";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { ROUTES } from "@/shared/config/routes";
import { cn } from "@/shared/lib/cn";
import { errorMessage } from "@/shared/lib/error-message";
import { count, dateTime } from "@/shared/lib/format";

import { BillingStatusBadge } from "./billing-status-badge";

const FILTERS: { value: BillingStatus | null; label: string }[] = [
  { value: null, label: "전체" },
  { value: "FAILED", label: "결제 실패" },
  { value: "PENDING", label: "대기" },
  { value: "PAID", label: "결제 완료" },
  { value: "REFUNDED", label: "환불" },
];

/**
 * 청구 목록.
 *
 * 정렬은 금액 큰 순이다(서버). 미수를 쫓는 것이 이 목록의 용도라 같은 미수라면 큰 건부터
 * 확인하는 편이 낫다.
 *
 * <p>월을 고를 수 있어야 한다. 청구는 <b>업체마다 자기 결제일</b>에 일어나므로 월초에는
 * 이번 달 목록이 거의 비어 있다 — 고정이면 운영자는 "청구가 하나도 없다"로 읽는다.
 */
export function RevenueRecordsTable({ periods }: { periods: string[] }) {
  const [period, setPeriod] = useState(periods[0] ?? "");
  const [status, setStatus] = useState<BillingStatus | null>(null);
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error, refetch } = useBillingRecordsQuery({
    period,
    status,
    page,
  });

  const select = (next: BillingStatus | null) => {
    setStatus(next);
    // 필터를 바꾸면 1페이지로 돌아간다. 안 그러면 3페이지에서 필터를 바꿨을 때
    // 결과가 1페이지뿐이라 빈 화면이 나온다.
    setPage(0);
  };

  return (
    <Card>
      <CardHeader
        title="청구 내역"
        aside={
          <div className="flex flex-wrap items-center gap-1.5">
            <select
              value={period}
              onChange={(e) => {
                setPeriod(e.target.value);
                setPage(0);
              }}
              aria-label="청구월"
              className="tabular mr-1 rounded-md border border-line bg-card px-2 py-1 text-[12.5px]"
            >
              {periods.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
            {FILTERS.map((chip) => {
              const active = status === chip.value;
              return (
                <button
                  key={chip.label}
                  type="button"
                  aria-pressed={active}
                  onClick={() => select(chip.value)}
                  className={cn(
                    "rounded-full border px-2.5 py-1 text-[12px] whitespace-nowrap transition-colors",
                    active
                      ? "border-ink bg-ink text-white"
                      : "border-line bg-card text-slate hover:border-ink-3",
                  )}
                >
                  {chip.label}
                </button>
              );
            })}
          </div>
        }
      />

      {isPending ? <LoadingState /> : null}
      {isError ? (
        <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
      ) : null}

      {data ? (
        data.content.length === 0 ? (
          <EmptyState
            message={
              status === null
                ? "이 달에 청구된 건이 없습니다"
                : "이 상태에 해당하는 청구가 없습니다"
            }
          />
        ) : (
          <>
            <Table>
              <thead>
                <tr>
                  <Th>업체</Th>
                  <Th className="w-[100px]">금액</Th>
                  <Th className="w-[104px]">상태</Th>
                  <Th className="w-[64px]">시도</Th>
                  <Th>실패 사유</Th>
                  <Th className="w-[140px]">결제 시각</Th>
                  <Th className="w-[76px]">영수증</Th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((item) => (
                  <tr key={item.id} className="hover:bg-line-2/40">
                    <Td>
                      <Link
                        href={`${ROUTES.tenants}/${item.tenant.id}`}
                        className="hover:underline"
                      >
                        <TitleCell
                          title={item.tenant.name}
                          sub={item.planName ?? "요금제 없음"}
                        />
                      </Link>
                    </Td>
                    <Td className="tabular text-[13px] font-medium">{count(item.amountKrw)}</Td>
                    <Td>
                      <BillingStatusBadge status={item.status} />
                    </Td>
                    <Td className="tabular text-[13px] text-slate">{item.attempts}</Td>
                    {/* 카드사 문구를 그대로 보여준다 — "한도 초과"는 업체가 바로 조치할 수 있는 정보다 */}
                    <Td className="text-[12.5px] text-slate">{item.failureReason ?? "—"}</Td>
                    <Td className="tabular text-[12.5px] text-slate">{dateTime(item.paidAt)}</Td>
                    <Td>
                      {item.receiptUrl ? (
                        <a
                          href={item.receiptUrl}
                          target="_blank"
                          rel="noreferrer noopener"
                          className="text-[12.5px] text-seal hover:underline"
                        >
                          보기
                        </a>
                      ) : (
                        <span className="text-[12.5px] text-slate-2">—</span>
                      )}
                    </Td>
                  </tr>
                ))}
              </tbody>
            </Table>

            {data.page.totalPages > 1 ? (
              <CardBody className="flex items-center gap-3 border-t border-line-2 pt-3">
                <Eyebrow>
                  {data.page.number + 1} / {data.page.totalPages} · 전체{" "}
                  {count(data.page.totalElements)}건
                </Eyebrow>
                <div className="ml-auto flex gap-2">
                  <Button
                    size="sm"
                    disabled={data.page.number === 0}
                    onClick={() => setPage((current) => Math.max(current - 1, 0))}
                  >
                    이전
                  </Button>
                  <Button
                    size="sm"
                    disabled={data.page.number >= data.page.totalPages - 1}
                    onClick={() => setPage((current) => current + 1)}
                  >
                    다음
                  </Button>
                </div>
              </CardBody>
            ) : null}
          </>
        )
      ) : null}
    </Card>
  );
}
