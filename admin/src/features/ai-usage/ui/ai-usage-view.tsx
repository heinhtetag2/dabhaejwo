"use client";

import {
  useAiUsageSummaryQuery,
  useDailyCostQuery,
  useModelUsageQuery,
  useTopTenantsQuery,
  type DailyCost,
  type UsagePurpose,
} from "@/entities/usage";
import { Card, CardBody, CardHeader, Eyebrow, Stat } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { errorMessage } from "@/shared/lib/error-message";
import { count, won } from "@/shared/lib/format";
import { PageHeader } from "@/widgets/page-header/page-header";

const PURPOSE_LABEL: Record<UsagePurpose, string> = {
  ANSWER: "답변 생성",
  EMBED_DOC: "문서 학습",
  EMBED_QUERY: "질문 벡터화",
  ETC: "기타",
};

/** 누적 막대의 층 순서는 화면 계약이다. 범례와 같은 순서를 쓴다. */
const LAYERS: { key: keyof Omit<DailyCost, "day">; label: string; className: string }[] = [
  { key: "answerKrw", label: "답변 생성", className: "bg-seal" },
  { key: "embedDocKrw", label: "문서 학습", className: "bg-mark" },
  { key: "embedQueryKrw", label: "질문 벡터화", className: "bg-plum" },
  { key: "etcKrw", label: "기타", className: "bg-slate-2" },
];

export function AiUsageView() {
  const summary = useAiUsageSummaryQuery();
  const daily = useDailyCostQuery(14);
  const models = useModelUsageQuery();
  const top = useTopTenantsQuery(5);

  if (summary.isPending) return <LoadingState />;
  if (summary.isError) {
    return (
      <ErrorState message={errorMessage(summary.error)} onRetry={() => void summary.refetch()} />
    );
  }

  const s = summary.data;

  return (
    <>
      <PageHeader title="AI 사용량" description="모델별 토큰과 비용" />

      <div className="mb-4 grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
        <Stat
          label="오늘 처리 토큰"
          value={count(s.todayTokensIn + s.todayTokensOut)}
          detail={`입력 ${count(s.todayTokensIn)} · 출력 ${count(s.todayTokensOut)}`}
        />
        <Stat label="오늘 모델 원가" value={count(Math.round(s.todayCostKrw))} detail="원" />
        <Stat
          label="대화 한 건당"
          // 대화가 없으면 정의되지 않는다. 0원으로 보이면 "공짜로 돌고 있다"로 읽힌다.
          value={s.costPerConvKrw === null ? "—" : `${s.costPerConvKrw}원`}
          detail={s.costPerConvKrw === null ? "오늘 대화 없음" : "저장 답변 포함 평균"}
        />
        <Stat
          label="이번 달 누적"
          value={count(Math.round(s.monthCostKrw))}
          detail={`예상 마감 ${count(Math.round(s.monthProjectedCostKrw))} (추정)`}
        />
      </div>

      <Card className="mb-4">
        <CardHeader title="최근 14일 원가" aside={<Eyebrow>용도별 누적</Eyebrow>} />
        <CardBody>
          <DailyChart data={daily.data ?? []} />
          <div className="mt-3.5 flex flex-wrap gap-4 text-[12px] text-slate">
            {LAYERS.map((layer) => (
              <span key={layer.key} className="flex items-center gap-1.5">
                <i aria-hidden className={`size-2.5 rounded-sm ${layer.className}`} />
                {layer.label}
              </span>
            ))}
          </div>
        </CardBody>
      </Card>

      <Card className="mb-4">
        <CardHeader title="모델별 사용량" aside={<Eyebrow>이번 달 기준</Eyebrow>} />
        {(models.data ?? []).length === 0 ? (
          <EmptyState message="모델 호출 기록이 없습니다. 답변 파이프라인이 붙으면 채워집니다" />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>모델</Th>
                <Th className="w-[96px]">용도</Th>
                <Th className="w-[76px]">호출</Th>
                <Th className="w-[96px]">입력 토큰</Th>
                <Th className="w-[86px]">출력 토큰</Th>
                <Th className="w-[100px]">원가</Th>
                <Th className="w-[80px]">비중</Th>
              </tr>
            </thead>
            <tbody>
              {(models.data ?? []).map((row) => (
                <tr key={`${row.provider}-${row.model}-${row.purpose}`}>
                  <Td>
                    <TitleCell title={row.model} sub={row.provider} />
                  </Td>
                  <Td className="text-[13px] text-slate">{PURPOSE_LABEL[row.purpose]}</Td>
                  <Td className="tabular text-[13px] text-slate">{count(row.callCount)}</Td>
                  <Td className="tabular text-[13px] text-slate">{count(row.inputTokens)}</Td>
                  <Td className="tabular text-[13px] text-slate">{count(row.outputTokens)}</Td>
                  <Td className="tabular text-[13px] text-slate">
                    {won(Math.round(row.costKrw))}
                  </Td>
                  <Td className="tabular text-[13px] text-slate">{row.sharePercent}%</Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>

      <Card>
        <CardHeader title="많이 쓰는 업체" aside={<Eyebrow>이번 달 원가순</Eyebrow>} />
        {(top.data ?? []).length === 0 ? (
          <EmptyState message="집계된 사용량이 없습니다" />
        ) : (
          <Table>
            <thead>
              <tr>
                <Th>업체</Th>
                <Th className="w-[88px]">토큰</Th>
                <Th className="w-[92px]">원가</Th>
                <Th className="w-[88px]">대화당</Th>
              </tr>
            </thead>
            <tbody>
              {(top.data ?? []).map((row) => (
                <tr key={row.tenant.id}>
                  <Td>
                    <TitleCell title={row.tenant.name} sub={row.planName ?? "요금제 없음"} />
                  </Td>
                  <Td className="tabular text-[13px] text-slate">{count(row.tokens)}</Td>
                  <Td className="tabular text-[13px] text-slate">
                    {won(Math.round(row.costKrw))}
                  </Td>
                  <Td className="tabular text-[13px] text-slate">
                    {row.costPerConvKrw === null ? "—" : `${row.costPerConvKrw}원`}
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>
    </>
  );
}

/**
 * 14일 누적 막대.
 *
 * 차트 라이브러리를 쓰지 않는 이유는 막대 14개에 번들을 얹을 이유가 없어서다.
 * 데이터가 없는 날도 0 으로 내려오므로 간격이 어긋나지 않는다.
 */
function DailyChart({ data }: { data: DailyCost[] }) {
  const totals = data.map((d) => d.answerKrw + d.embedDocKrw + d.embedQueryKrw + d.etcKrw);
  const max = Math.max(...totals, 1);

  if (data.length === 0) {
    return <p className="py-8 text-center text-[13px] text-slate-2">집계된 원가가 없습니다</p>;
  }

  return (
    <div>
      <div className="flex h-[110px] items-end gap-[5px]">
        {data.map((day, index) => (
          <div
            key={day.day}
            className="flex flex-1 flex-col justify-end gap-px"
            title={`${day.day} · ${won(Math.round(totals[index]))}`}
          >
            {[...LAYERS].reverse().map((layer) => (
              <div
                key={layer.key}
                className={layer.className}
                style={{ height: `${Math.round((day[layer.key] / max) * 104)}px` }}
              />
            ))}
          </div>
        ))}
      </div>
      <div className="mt-2 flex gap-[5px]">
        {data.map((day) => (
          <span
            key={day.day}
            className="tabular flex-1 text-center text-[10px] text-slate-2"
          >
            {day.day.slice(8)}
          </span>
        ))}
      </div>
    </div>
  );
}
