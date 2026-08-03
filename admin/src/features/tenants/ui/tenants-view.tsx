"use client";

import { useState } from "react";

import { useTenantListQuery, type TenantSort } from "@/entities/tenant";
import { Card, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { count, won } from "@/shared/lib/format";
import { CostRatioBar } from "@/shared/ui/cost-ratio-bar";
import { TenantStatusBadge } from "@/shared/ui/tenant-status-badge";
import { PageHeader } from "@/widgets/page-header/page-header";

const SORT_LABEL: Record<TenantSort, string> = {
  COST_RATIO_DESC: "원가율 높은 순",
  NAME_ASC: "가나다순",
  CONV_DESC: "대화 많은 순",
};

export function TenantsView() {
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<TenantSort>("COST_RATIO_DESC");

  const { data, isPending, isError, error, refetch } = useTenantListQuery({ q: query, sort });

  return (
    <>
      <PageHeader
        title="업체"
        description="계정 관리와 CS 대응의 중심 화면"
        actions={
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            // 문의 메일에 적힌 도메인만으로 찾을 수 있어야 한다 (tenant-plan.md §4.1.2)
            placeholder="업체명, 도메인, 키로 찾기"
            aria-label="업체 검색"
            className="w-[230px] rounded-[7px] border border-line bg-card px-2.5 py-1.5 text-[13px]"
          />
        }
      />

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
          <ErrorState
            message={error instanceof Error ? error.message : "업체를 불러오지 못했습니다"}
            onRetry={() => void refetch()}
          />
        ) : null}

        {data && data.content.length === 0 ? (
          <EmptyState message="조건에 맞는 업체가 없습니다" />
        ) : null}

        {data && data.content.length > 0 ? (
          <div className="px-1 pt-3.5 pb-1">
            <table className="w-full border-collapse">
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
                  <tr key={tenant.id} className="hover:bg-line-2/40">
                    <Td>
                      <div className="font-medium">{tenant.name}</div>
                      <div className="tabular text-[11.5px] text-slate-2">
                        {tenant.primaryDomain}
                      </div>
                    </Td>
                    <Td className="text-[13px] text-slate">{tenant.plan?.name ?? "—"}</Td>
                    <Td className="tabular text-[13px] text-slate">
                      {count(tenant.convCount)}
                    </Td>
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
            </table>
          </div>
        ) : null}
      </Card>
    </>
  );
}

function Th({ className, children }: { className?: string; children: React.ReactNode }) {
  return (
    <th
      className={`border-b border-line-2 px-3.5 pb-2.5 text-left font-mono text-[10.5px] font-medium tracking-[0.09em] text-slate-2 uppercase ${className ?? ""}`}
    >
      {children}
    </th>
  );
}

function Td({ className, children }: { className?: string; children: React.ReactNode }) {
  return (
    <td className={`border-b border-line-2 px-3.5 py-2.5 align-middle ${className ?? ""}`}>
      {children}
    </td>
  );
}
