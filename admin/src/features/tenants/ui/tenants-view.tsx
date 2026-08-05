"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

import {
  useTenantFilterCountsQuery,
  useTenantListQuery,
  type TenantFilter,
  type TenantFilterCounts,
  type TenantSort,
} from "@/entities/tenant";
import { Card, CardHeader, Eyebrow } from "@/shared/common/card";
import { TextInput } from "@/shared/common/field";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { cn } from "@/shared/lib/cn";
import { errorMessage } from "@/shared/lib/error-message";
import { count, won } from "@/shared/lib/format";
import { CostRatioBar } from "@/shared/ui/cost-ratio-bar";
import { TenantStatusBadge } from "@/shared/ui/tenant-status-badge";
import { PageHeader } from "@/widgets/page-header/page-header";

import { TenantDetailPanel } from "./tenant-detail-panel";

const SORT_LABEL: Record<TenantSort, string> = {
  COST_RATIO_DESC: "원가율 높은 순",
  NAME_ASC: "가나다순",
  CONV_DESC: "대화 많은 순",
};

/** 칩 순서는 기획서 §4.1.1 표 순서 그대로다. */
const FILTERS: { value: TenantFilter; label: string; key: keyof TenantFilterCounts }[] = [
  { value: "ALL", label: "전체", key: "all" },
  { value: "TRIAL", label: "체험 중", key: "trial" },
  { value: "PAYMENT_FAILED", label: "결제 실패", key: "paymentFailed" },
  { value: "COST_EXCEEDED", label: "원가 초과", key: "costExceeded" },
  { value: "INACTIVE_7D", label: "7일 미접속", key: "inactive7d" },
  { value: "SUSPENDED", label: "일시정지", key: "suspended" },
  { value: "CHURNED", label: "해지", key: "churned" },
];

/**
 * 업체 목록과 상세.
 *
 * 좌우 분할인 이유는 별도 페이지로 이동하면 목록의 필터·스크롤이 초기화되어
 * 여러 업체를 연속 확인하는 CS 업무 흐름이 끊기기 때문이다
 * (admin-console-tenant-plan.md §3).
 */
export function TenantsView() {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<TenantFilter>("ALL");
  const [sort, setSort] = useState<TenantSort>("COST_RATIO_DESC");

  // 선택은 URL 에 둔다. 알림·감사 기록이 `?tenantId=` 로 이 화면의 <b>특정 업체</b>를
  // 가리키기 때문이다 — 상태로만 들고 있으면 그 링크가 목록 첫 화면에 떨어진다.
  const router = useRouter();
  const searchParams = useSearchParams();
  const selectedId = searchParams.get("tenantId");
  const setSelectedId = (tenantId: string) => {
    const next = new URLSearchParams(searchParams.toString());
    next.set("tenantId", tenantId);
    // replace 인 이유 — 목록에서 업체를 훑어볼 때마다 뒤로가기 기록이 쌓이면
    // 화면을 벗어나려고 뒤로가기를 여러 번 눌러야 한다.
    router.replace(`?${next.toString()}`, { scroll: false });
  };

  const { data, isPending, isError, error, refetch } = useTenantListQuery({
    q: query,
    filter,
    sort,
  });
  const { data: counts } = useTenantFilterCountsQuery();

  return (
    <>
      <PageHeader
        title="업체"
        description="계정 관리와 CS 대응의 중심 화면"
        actions={
          <TextInput
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            // 문의 메일에 적힌 도메인만으로 찾을 수 있어야 한다 (§4.1.2)
            placeholder="업체명, 도메인, 키로 찾기"
            aria-label="업체 검색"
            className="w-[230px] px-2.5 py-1.5 text-[13px]"
          />
        }
      />

      {/* 건수가 0인 칩도 숨기지 않는다 — 사라지면 "그 상태가 없다"와 "그 필터가 없다"를 구분할 수 없다 */}
      <div className="mb-3.5 flex flex-wrap gap-1.5">
        {FILTERS.map((chip) => {
          const chipCount = counts?.[chip.key];
          const active = filter === chip.value;
          return (
            <button
              key={chip.value}
              type="button"
              aria-pressed={active}
              onClick={() => setFilter(chip.value)}
              className={cn(
                "rounded-full border px-3 py-[5px] text-[12.5px] transition-colors",
                active
                  ? "border-ink bg-ink text-white"
                  : "border-line bg-card text-slate hover:border-ink-3",
                !active && chipCount === 0 && "opacity-50",
              )}
            >
              {chip.label}
              {chipCount === undefined ? "" : ` ${chipCount}`}
            </button>
          );
        })}
      </div>

      <div className="grid items-start gap-4 xl:grid-cols-[1fr_348px]">
        <Card>
          <CardHeader
            title="업체"
            aside={
              <label className="flex items-center gap-2">
                <Eyebrow>정렬</Eyebrow>
                <select
                  value={sort}
                  onChange={(e) => setSort(e.target.value as TenantSort)}
                  className="rounded-md border border-line bg-card px-2 py-1 text-[12.5px]"
                >
                  {Object.entries(SORT_LABEL).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
            }
          />

          {isPending ? <LoadingState /> : null}

          {isError ? (
            <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
          ) : null}

          {data && data.content.length === 0 ? (
            <EmptyState message="조건에 맞는 업체가 없습니다" />
          ) : null}

          {data && data.content.length > 0 ? (
            <Table>
              <thead>
                <tr>
                  <Th>업체</Th>
                  <Th className="w-[88px]">요금제</Th>
                  <Th className="w-[92px]">이번 달 대화</Th>
                  <Th className="w-[150px]">원가율</Th>
                  <Th className="w-[74px]">상태</Th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((tenant) => (
                  <tr
                    key={tenant.id}
                    className={cn(
                      "hover:bg-line-2/40",
                      selectedId === tenant.id && "bg-line-2/60",
                    )}
                  >
                    <Td>
                      {/* 프로토타입은 <tr onclick> 이라 키보드로 못 고른다. 셀 안에 버튼을 둔다 */}
                      <button
                        type="button"
                        onClick={() => setSelectedId(tenant.id)}
                        aria-pressed={selectedId === tenant.id}
                        className="text-left"
                      >
                        <TitleCell title={tenant.name} sub={tenant.primaryDomain} />
                      </button>
                    </Td>
                    <Td className="text-[13px] text-slate">{tenant.plan?.name ?? "—"}</Td>
                    <Td className="tabular text-[13px] text-slate">{count(tenant.convCount)}</Td>
                    <Td>
                      <CostRatioBar percent={tenant.costRatioPercent} />
                      <span className="sr-only">
                        {won(tenant.costKrw)} / {won(tenant.billedKrw)}
                      </span>
                    </Td>
                    <Td>
                      <TenantStatusBadge status={tenant.status} />
                    </Td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ) : null}
        </Card>

        <div className="xl:sticky xl:top-4">
          <TenantDetailPanel tenantId={selectedId} />
        </div>
      </div>
    </>
  );
}
