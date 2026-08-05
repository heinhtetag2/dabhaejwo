"use client";

import { useTodayQuery, type ActionType, type TodaySystem } from "@/entities/today";
import { Badge, type BadgeTone } from "@/shared/common/badge";
import { LinkButton } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow, Stat } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, TitleCell } from "@/shared/common/table";
import { ROUTES } from "@/shared/config/routes";
import { errorMessage } from "@/shared/lib/error-message";
import { count, dateTime, relative } from "@/shared/lib/format";
import { PageHeader } from "@/widgets/page-header/page-header";

const ACTION_LABEL: Record<ActionType, { text: string; tone: BadgeTone }> = {
  COST_EXCEEDED: { text: "원가 초과", tone: "error" },
  JOB_FAILED: { text: "작업 실패", tone: "error" },
  PAYMENT_FAILED: { text: "결제 실패", tone: "warn" },
  TRIAL_ENDING: { text: "체험 종료", tone: "warn" },
  TICKET_WAITING: { text: "문의", tone: "info" },
};

export function TodayView() {
  const { data, isPending, isError, error, refetch } = useTodayQuery();

  if (isPending) return <LoadingState />;
  if (isError) {
    return <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />;
  }

  const { headline, stats, actions, system, aggregatedAt } = data;

  return (
    <>
      <PageHeader
        title="오늘"
        description={
          aggregatedAt
            ? `사용량 집계 기준 ${dateTime(aggregatedAt)}`
            : "사용량이 아직 한 번도 집계되지 않았습니다"
        }
      />

      {/* 지표를 나열하면 무엇이 중요한지 사라진다. 최상단에는 문장 하나만 크게 둔다
          (admin-console-plan.md §2.2) */}
      <section className="mb-5 rounded-card border border-line bg-card px-[30px] py-7">
        <Eyebrow className="mb-3 block">지금 봐야 할 것</Eyebrow>
        {headline.costExceededCount > 0 ? (
          <h2 className="max-w-[660px] text-[27px] leading-[1.45] font-semibold tracking-[-0.03em]">
            업체 <b className="tabular text-[29px] font-bold">{headline.tenantCount}</b>곳 중{" "}
            <span className="bg-gradient-to-t from-brick-soft from-[42%] to-transparent to-[42%] px-0.5">
              <b className="tabular text-[29px] font-bold">{headline.costExceededCount}</b>곳은 이번
              달 원가가 요금을 넘겼습니다.
            </span>
          </h2>
        ) : (
          <h2 className="max-w-[660px] text-[27px] leading-[1.45] font-semibold tracking-[-0.03em]">
            업체 <b className="tabular text-[29px] font-bold">{headline.tenantCount}</b>곳 모두 원가가
            요금 안에 있습니다.
          </h2>
        )}
        <div className="mt-5 flex flex-wrap items-center gap-2.5">
          <LinkButton href={ROUTES.profitability} variant="primary">
            수익성 확인
          </LinkButton>
          <span className="text-[12px] text-slate-2">
            원가가 튀는 업체는 대부분 공통 질문이 0~2개입니다.
          </span>
        </div>
      </section>

      <div className="mb-5 grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="유료 업체" value={count(stats.payingTenantCount)} detail="상태 정상" />
        <Stat label="월 반복 매출" value={count(stats.mrrKrw)} detail="원" />
        <Stat label="오늘 총 대화" value={count(stats.todayConvCount)} detail="집계 기준" />
        <Stat
          label="오늘 모델 원가"
          value={count(Math.round(stats.todayCostKrw))}
          detail="원"
          tone={stats.todayCostKrw > 0 ? "down" : "neutral"}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="지금 처리해야 할 일" />
          {actions.length === 0 ? (
            <EmptyState message="조치가 필요한 항목이 없습니다" />
          ) : (
            <Table>
              <tbody>
                {actions.map((action, index) => {
                  const label = ACTION_LABEL[action.type];
                  return (
                    <tr key={`${action.type}-${action.tenantId ?? index}`}>
                      <Td className="w-[96px]">
                        <Badge tone={label.tone}>{label.text}</Badge>
                      </Td>
                      <Td>
                        <TitleCell title={action.title} sub={action.detail} />
                      </Td>
                      <Td className="w-[96px] text-right">
                        {/* 누르면 바로 그 대상으로 간다 — 이름만 확인하고 다시 검색하게 만들지 않는다 */}
                        <LinkButton href={action.targetPath} size="sm">
                          보기
                        </LinkButton>
                      </Td>
                    </tr>
                  );
                })}
              </tbody>
            </Table>
          )}
        </Card>

        <SystemCard system={system} />
      </div>
    </>
  );
}

/**
 * 시스템 상태.
 *
 * 측정 지점이 없는 값은 서버가 `null` 로 준다. 0으로 그리면 "응답 0ms, 오류 0건"이라는
 * 거짓이 되고 운영자는 정상이라고 읽는다 — "집계 없음"이라고 밝힌다.
 */
function SystemCard({ system }: { system: TodaySystem }) {
  const measured = system.chatApiP95Ms !== null || system.todayError5xxCount !== null;

  return (
    <Card>
      <CardHeader
        title="시스템"
        aside={<Badge tone={measured ? "ok" : "idle"}>{measured ? "정상" : "미측정"}</Badge>}
      />
      <CardBody>
        <Row label="챗 API 응답 (p95)" value={system.chatApiP95Ms} suffix="ms" />
        <Row label="임베딩 큐 대기" value={system.embedQueueDepth} suffix="건" />
        <Row label="크롤러 워커" value={system.crawlerWorkers} />
        <Row label="벡터 DB 사용량" value={system.vectorDbUsagePercent} suffix="%" />
        <Row label="오늘 5xx 오류" value={system.todayError5xxCount} suffix="건" />

        <div className="mt-4 border-t border-line-2 pt-3.5">
          <Eyebrow className="mb-2 block">최근 오류</Eyebrow>
          {system.recentErrors.length === 0 ? (
            <p className="font-mono text-[11.5px] text-slate-2">
              기록된 작업 실패가 없습니다
            </p>
          ) : (
            <ul className="space-y-1 font-mono text-[11.5px] text-slate">
              {system.recentErrors.map((item) => (
                <li key={`${item.at}-${item.code}`}>
                  {relative(item.at)} <b className="font-medium text-ink">{item.tenantName}</b>{" "}
                  {item.code}
                </li>
              ))}
            </ul>
          )}
        </div>

        {!measured ? (
          <p className="mt-4 border-t border-line-2 pt-3.5 text-[11.5px] leading-relaxed text-slate-2">
            응답 시간·워커 가동·벡터 DB·5xx 는 아직 측정 지점이 없습니다. 답변 파이프라인과
            워커가 붙으면 값이 채워집니다.
          </p>
        ) : null}
      </CardBody>
    </Card>
  );
}

function Row({
  label,
  value,
  suffix,
}: {
  label: string;
  value: number | string | null;
  suffix?: string;
}) {
  return (
    <div className="flex justify-between gap-3 border-b border-line-2 py-2 text-[13px] last:border-b-0">
      <span className="text-slate">{label}</span>
      <span className="tabular">
        {value === null ? (
          <span className="text-slate-2">집계 없음</span>
        ) : (
          `${typeof value === "number" ? count(value) : value}${suffix ?? ""}`
        )}
      </span>
    </div>
  );
}
