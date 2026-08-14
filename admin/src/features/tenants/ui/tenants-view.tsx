"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";

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
import { ROUTES } from "@/shared/config/routes";
import { cn } from "@/shared/lib/cn";
import { errorMessage } from "@/shared/lib/error-message";
import { count, won } from "@/shared/lib/format";
import { CostRatioBar } from "@/shared/ui/cost-ratio-bar";
import { TenantStatusBadge } from "@/shared/ui/tenant-status-badge";
import { PageHeader } from "@/widgets/page-header/page-header";

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
 * 업체 목록.
 *
 * <p>상세는 <b>별도 페이지</b>다({@code /tenants/[id]}). 원래는 좌우 분할이었는데,
 * 상세에 담을 것이 늘면서 좁은 칸에 다 밀어넣게 되어 목록·상세 양쪽이 답답해졌다.
 *
 * <p>페이지를 나누면 "여러 업체를 연속으로 확인하는 CS 흐름이 끊긴다"는 문제가 생긴다
 * (admin-console-tenant-plan.md §3 이 좌우 분할을 택한 이유). 그래서
 * <b>검색어·필터·정렬을 전부 URL 에 둔다</b> — 상세에서 돌아오면 보던 목록 그대로다.
 */
export function TenantsView() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // 상태를 useState 로 들면 상세를 다녀올 때 초기화된다. URL 이 곧 상태다.
  const query = searchParams.get("q") ?? "";
  const filter = (searchParams.get("filter") as TenantFilter | null) ?? "ALL";
  const sort = (searchParams.get("sort") as TenantSort | null) ?? "COST_RATIO_DESC";

  const setParam = (key: string, value: string, fallback: string) => {
    const next = new URLSearchParams(searchParams.toString());
    if (value === fallback) {
      next.delete(key);
    } else {
      next.set(key, value);
    }
    // replace 인 이유 — 필터를 바꿀 때마다 뒤로가기 기록이 쌓이면
    // 화면을 벗어나려고 뒤로가기를 여러 번 눌러야 한다.
    router.replace(next.toString() ? `?${next.toString()}` : ROUTES.tenants, { scroll: false });
  };

  const { data, isPending, isError, error, refetch } = useTenantListQuery({ q: query, filter, sort });
  const { data: counts } = useTenantFilterCountsQuery();

  /** 상세로 갈 때 목록 상태를 그대로 들고 간다. 돌아오면 보던 화면이다. */
  const detailHref = (tenantId: string) => {
    const back = searchParams.toString();
    return back
      ? `${ROUTES.tenants}/${tenantId}?back=${encodeURIComponent(back)}`
      : `${ROUTES.tenants}/${tenantId}`;
  };

  return (
    <>
      <PageHeader
        title="업체"
        description="계정 관리와 CS 대응의 중심 화면"
        actions={
          <TextInput
            value={query}
            onChange={(e) => setParam("q", e.target.value, "")}
            // 문의 메일에 적힌 도메인만으로 찾을 수 있어야 한다 (§4.1.2)
            placeholder="업체명, 서비스명, 도메인, 키로 찾기"
            aria-label="업체 검색"
            className="w-57.5 px-2.5 py-1.5 text-[13px]"
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
              onClick={() => setParam("filter", chip.value, "ALL")}
              className={cn(
                "rounded-full border px-3 py-1.25 text-[12.5px] whitespace-nowrap transition-colors",
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

      <Card>
        <CardHeader
          title="업체"
          aside={
            <label className="flex items-center gap-2">
              <Eyebrow>정렬</Eyebrow>
              <select
                value={sort}
                onChange={(e) => setParam("sort", e.target.value, "COST_RATIO_DESC")}
                aria-label="정렬"
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
                <Th className="w-27.5">요금제</Th>
                <Th className="w-28">이번 달 대화</Th>
                <Th className="w-42.5">원가율</Th>
                <Th className="w-24">상태</Th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((tenant) => (
                <tr key={tenant.id} className="hover:bg-line-2/40">
                  <Td>
                    {/*
                     * 프로토타입은 <tr onclick> 이라 키보드로 못 고른다. 셀 안에 링크를 둔다 —
                     * 버튼이 아니라 링크여야 새 탭으로 열고 주소를 복사할 수 있다.
                     */}
                    <Link href={detailHref(tenant.id)} className="block text-left">
                      <TitleCell
                      title={tenant.name}
                      /*
                        서비스가 여럿이면 대표 도메인만 보여주는 것은 거짓말이 된다 —
                        위젯이 도는 주소가 여럿이기 때문이다. 1이면 지금과 글자 하나 다르지 않다.
                      */
                      sub={
                        tenant.botCount > 1
                          ? `${tenant.primaryDomain} 외 ${tenant.botCount - 1}`
                          : tenant.primaryDomain
                      }
                    />
                    </Link>
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
    </>
  );
}
