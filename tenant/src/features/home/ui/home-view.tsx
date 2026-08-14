"use client";

import { useHomeSummaryQuery } from "@/entities/dashboard/home";
import { LinkButton } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow, Stat } from "@/shared/common/card";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { botRoute } from "@/shared/config/routes";

import { KnowledgeStrip } from "./knowledge-strip";
import { useCurrentBotId } from "@/shared/lib/current-bot";

/**
 * 홈. 화면의 중심은 통계가 아니라 <b>오늘 할 일 하나</b>다 (tenant-plan.md §2.1).
 * 그래서 지표 카드보다 헤드라인 문장이 먼저 온다.
 */
export function HomeView() {
  const botId = useCurrentBotId();
  const { data, isPending, isError, refetch } = useHomeSummaryQuery();

  if (isPending) {
    return <LoadingState />;
  }
  if (isError) {
    return <ErrorState message="홈 정보를 불러오지 못했습니다" onRetry={() => void refetch()} />;
  }

  return (
    <>
      <Headline convCount={data.todayConvCount} gapCount={data.openGapCount} />

      <div className="mb-5.5 grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
        <Stat
          label="오늘 대화"
          value={data.todayConvCount.toLocaleString()}
          detail={deltaLabel(data.todayConvDelta)}
          tone={data.todayConvDelta > 0 ? "up" : data.todayConvDelta < 0 ? "down" : "neutral"}
        />
        <Stat
          label="답변 성공률"
          value={data.answerSuccessPercent === null ? "—" : `${data.answerSuccessPercent}%`}
          detail={
            data.answerSuccessPercentLastWeek === null
              ? "지난주 기록 없음"
              : `지난주 ${data.answerSuccessPercentLastWeek}%`
          }
        />
        <Stat
          label="남긴 연락처"
          value={data.todayLeadCount.toLocaleString()}
          detail={`이번 주 누적 ${data.weekLeadCount.toLocaleString()}`}
        />
        <Stat
          label="평균 응답"
          value={data.avgResponseMs === null ? "—" : `${(data.avgResponseMs / 1000).toFixed(1)}초`}
          // TODO(stub): 답변 파이프라인이 없어 응답 시간을 잰 적이 없다. 값을 지어내지 않는다.
          detail={data.avgResponseMs === null ? "아직 측정된 대화가 없습니다" : "정상 범위"}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader
            title="지식 상태"
            aside={<Eyebrow>{data.knowledge.documentCount.toLocaleString()}개 문서</Eyebrow>}
          />
          <CardBody>
            <KnowledgeStrip knowledge={data.knowledge} />
            <div className="mt-4 flex flex-wrap gap-2 border-t border-line-2 pt-4">
              <LinkButton size="sm" href={botRoute(botId, "sources")}>
                소스 관리
              </LinkButton>
              {data.knowledge.failedCount > 0 ? (
                <LinkButton size="sm" href={botRoute(botId, "sources")}>
                  실패 {data.knowledge.failedCount}건 확인
                </LinkButton>
              ) : null}
            </div>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="많이 물어본 질문" aside={<Eyebrow>최근 7일</Eyebrow>} />
          {data.topQuestions.length === 0 ? (
            <CardBody className="text-[13px] text-slate-2">
              아직 들어온 질문이 없습니다. 챗봇을 설치하면 여기에 쌓입니다.
            </CardBody>
          ) : (
            <ol className="px-[18px] py-2">
              {data.topQuestions.map((item, index) => (
                <li
                  key={item.question}
                  className="flex items-center gap-3 border-b border-line-2 py-3 text-[13.5px] last:border-b-0"
                >
                  <span className="tabular w-6 shrink-0 text-[11.5px] text-slate-2">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <span className="min-w-0 flex-1 truncate">{item.question}</span>
                  <span className="tabular shrink-0 text-[11.5px] text-slate-2">
                    {item.askCount.toLocaleString()}
                  </span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>
    </>
  );
}

/**
 * 오늘의 한 줄. 지표를 나열하면 무엇을 해야 할지 사라지므로 문장 하나만 크게 둔다.
 * 이 문장이 곧 행동 유도다.
 */
function Headline({ convCount, gapCount }: { convCount: number; gapCount: number }) {
  const botId = useCurrentBotId();
  const nothingToDo = gapCount === 0;

  return (
    <section className="mb-5.5 rounded-card border border-line bg-card px-8 py-7.5">
      <Eyebrow>오늘의 한 줄</Eyebrow>
      <h2 className="mt-2.5 text-[26px] leading-[1.45] font-semibold tracking-[-0.02em]">
        {nothingToDo ? (
          <>
            오늘 들어온 <b className="font-semibold">{convCount.toLocaleString()}</b>건의 질문에
            <br />
            <span className="bg-linear-to-t from-mark-soft from-42% to-42% to-transparent px-0.5">
              모두 답했습니다.
            </span>
          </>
        ) : (
          <>
            오늘 들어온 <b className="font-semibold">{convCount.toLocaleString()}</b>건의 질문 중
            <br />
            <span className="bg-linear-to-t from-mark-soft from-42% to-42% to-transparent px-0.5">
              <b className="font-semibold">{gapCount.toLocaleString()}</b>건은 답하지 못했습니다.
            </span>
          </>
        )}
      </h2>

      {nothingToDo ? (
        <p className="mt-4 text-[12.5px] text-slate">
          답을 못 한 질문이 생기면 여기에 먼저 알려드립니다.
        </p>
      ) : (
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <LinkButton variant="accent" href={botRoute(botId, "improve")}>
            {gapCount}건 채우러 가기
          </LinkButton>
          <span className="text-[12.5px] text-slate">
            답 못 한 질문에 한 번 답을 달아두면, 다음부터는 챗봇이 대신 답합니다.
          </span>
        </div>
      )}
    </section>
  );
}

function deltaLabel(delta: number): string {
  if (delta === 0) {
    return "어제와 같음";
  }
  return delta > 0 ? `▲ 어제보다 +${delta}` : `▼ 어제보다 ${delta}`;
}
