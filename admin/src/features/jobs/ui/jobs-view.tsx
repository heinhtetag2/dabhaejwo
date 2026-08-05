"use client";

import { useJobListQuery, useJobStatsQuery, useRetryJob } from "@/entities/job";
import { Badge } from "@/shared/common/badge";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Stat } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { errorMessage } from "@/shared/lib/error-message";
import { count, dateTime } from "@/shared/lib/format";
import { PageHeader } from "@/widgets/page-header/page-header";

/**
 * 오류 코드는 운영자용이라 원문 그대로 보여주고 한글 설명을 병기한다
 * (admin-console-plan.md §4.5).
 */
const ERROR_HINT: Record<string, string> = {
  pdf_parse_timeout: "대용량·복잡한 PDF — 타임아웃 상향 후 재시도",
  no_text_extracted: "스캔 문서 또는 이미지 PDF — 업체에 OCR 안내",
  http_404: "삭제된 페이지 — 소스에서 자동 제외",
  robots_disallow: "크롤링 차단 설정 — robots.txt 수정 안내",
  rate_limited: "모델 API 한도 — 자동 백오프 재시도",
};

const KIND_LABEL: Record<string, string> = {
  CRAWL: "크롤링",
  RECRAWL: "재크롤링",
  EMBED_DOC: "임베딩",
};

export function JobsView() {
  const stats = useJobStatsQuery();
  const jobs = useJobListQuery("FAILED");
  const retry = useRetryJob();

  if (stats.isPending) return <LoadingState />;
  if (stats.isError) {
    return <ErrorState message={errorMessage(stats.error)} onRetry={() => void stats.refetch()} />;
  }

  const s = stats.data;

  return (
    <>
      <PageHeader title="작업 큐" description="크롤링·임베딩 실패 복구" />

      <div className="mb-4 grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="대기" value={count(s.queuedCount)} detail="건" />
        <Stat label="진행 중" value={count(s.runningCount)} detail="건" />
        <Stat
          label="오늘 완료"
          value={count(s.doneTodayCount)}
          // 오늘 처리된 작업이 없으면 성공률이 정의되지 않는다 — 0%로 보이면 전부 실패로 읽힌다
          detail={s.successPercent === null ? "오늘 처리 없음" : `성공률 ${s.successPercent}%`}
          tone={s.successPercent === null ? "neutral" : "up"}
        />
        <Stat
          label="실패"
          value={count(s.failedCount)}
          detail="재시도 소진"
          tone={s.failedCount > 0 ? "down" : "neutral"}
        />
      </div>

      <Card>
        <CardHeader
          title="실패한 작업"
          aside={
            <Button
              size="sm"
              disabled={s.failedCount === 0 || retry.isPending}
              onClick={() => retry.mutate("all")}
            >
              전체 재시도
            </Button>
          }
        />

        {/* 재시도는 지금 항상 거절된다. 그 사실을 화면이 그대로 보여준다 —
            조용히 성공시키면 운영자는 복구된 줄 알고 기다린다 */}
        {retry.isError ? (
          <CardBody className="border-b border-line-2 py-3">
            <p className="text-[12.5px] text-brick">{errorMessage(retry.error)}</p>
          </CardBody>
        ) : null}

        {jobs.isPending ? <LoadingState /> : null}

        {jobs.data && jobs.data.content.length === 0 ? (
          <EmptyState message="실패한 작업이 없습니다. 크롤러·임베딩 워커가 아직 붙지 않아 큐는 비어 있습니다" />
        ) : null}

        {jobs.data && jobs.data.content.length > 0 ? (
          <Table>
            <thead>
              <tr>
                <Th className="w-[96px]">작업</Th>
                <Th>대상</Th>
                <Th>오류</Th>
                <Th className="w-[82px]">재시도</Th>
                <Th className="w-[110px]">시각</Th>
                <Th className="w-[76px]" />
              </tr>
            </thead>
            <tbody>
              {jobs.data.content.map((job) => (
                <tr key={job.id}>
                  <Td>
                    <Badge tone="idle" dot={false}>
                      {KIND_LABEL[job.kind] ?? job.kind}
                    </Badge>
                  </Td>
                  <Td>
                    <TitleCell title={job.tenant?.name ?? "—"} sub={job.target} />
                  </Td>
                  <Td>
                    <div className="tabular text-[12px] text-brick">{job.errorCode ?? "—"}</div>
                    {job.errorCode && ERROR_HINT[job.errorCode] ? (
                      <div className="text-[11.5px] text-slate-2">
                        {ERROR_HINT[job.errorCode]}
                      </div>
                    ) : null}
                  </Td>
                  <Td className="tabular text-[12px] text-slate-2">
                    {job.attempts} / {job.maxAttempts}
                  </Td>
                  <Td className="tabular text-[12px] text-slate-2">{dateTime(job.updatedAt)}</Td>
                  <Td>
                    <Button size="sm" disabled={retry.isPending} onClick={() => retry.mutate(job.id)}>
                      재시도
                    </Button>
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
