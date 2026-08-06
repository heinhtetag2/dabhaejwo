"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  GAP_REASON_LABEL,
  useAnswerGapsQuery,
  useDismissGap,
  useResolveGap,
  type AnswerGap,
  type GapStatus,
} from "@/entities/ops/answer-gap";
import { Button } from "@/shared/common/button";
import { Card, CardBody } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { canEdit } from "@/shared/lib/auth-store";
import { cn } from "@/shared/lib/cn";
import { Notice } from "@/shared/ui/notice";
import { Pagination } from "@/shared/ui/pagination";
import { StatusBadge } from "@/shared/ui/status-badge";
import { controlClass } from "@/shared/common/control";
import { dateTime } from "@/shared/lib/format";

const TABS: Array<{ value: GapStatus; label: string }> = [
  { value: "OPEN", label: "남은 질문" },
  { value: "RESOLVED", label: "답변 등록됨" },
  { value: "DISMISSED", label: "넘어간 질문" },
];

/**
 * 답변 개선 — 이 서비스의 심장이다 (tenant-plan.md §4.2).
 *
 * <p>개선 루프가 여기서 닫힌다. 답을 등록하면 공통 질문이 되어 다음 질문부터 바로 쓰이고,
 * 그 답변은 모델을 거치지 않으므로 원가가 들지 않는다.
 */
export function ImproveView() {
  const { data: context } = useAppContextQuery();
  const [status, setStatus] = useState<GapStatus>("OPEN");
  const [page, setPage] = useState(0);
  const [openId, setOpenId] = useState<number | null>(null);

  const { data, isPending, isError, refetch } = useAnswerGapsQuery(status, page);
  const editable = canEdit(context?.member?.role);

  return (
    <>
      <p className="mb-5 max-w-[620px] text-[13.5px] text-slate">
        챗봇이 답을 찾지 못했거나, 방문자가 👎를 누른 질문입니다. 답을 등록하면 다음 질문부터
        바로 사용되며, 그 답변은 <b className="font-semibold text-ink">모델을 거치지 않아 사용량에
        잡히지 않습니다.</b>
      </p>

      <div role="tablist" aria-label="상태" className="mb-4 flex flex-wrap gap-1">
        {TABS.map((tab) => (
          <button
            key={tab.value}
            role="tab"
            aria-selected={status === tab.value}
            onClick={() => {
              setStatus(tab.value);
              setPage(0);
              setOpenId(null);
            }}
            className={cn(
              "rounded-[7px] px-3 py-1.5 text-[13px] transition-colors",
              status === tab.value
                ? "bg-ink text-white"
                : "border border-line bg-card hover:bg-line-2/60",
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isPending ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState message="목록을 불러오지 못했습니다" onRetry={() => void refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState
          message={
            status === "OPEN"
              ? "답하지 못한 질문이 없습니다. 챗봇이 잘 답하고 있습니다."
              : "해당하는 질문이 없습니다."
          }
        />
      ) : (
        <>
          <div className="space-y-2.5">
            {data.content.map((gap) => (
              <GapCard
                key={gap.id}
                gap={gap}
                open={openId === gap.id}
                editable={editable}
                onToggle={() => setOpenId(openId === gap.id ? null : gap.id)}
              />
            ))}
          </div>
          <Card className="mt-3">
            <Pagination
              page={data.page.number}
              totalPages={data.page.totalPages}
              totalElements={data.page.totalElements}
              onChange={setPage}
            />
          </Card>
        </>
      )}
    </>
  );
}

function GapCard({
  gap,
  open,
  editable,
  onToggle,
}: {
  gap: AnswerGap;
  open: boolean;
  editable: boolean;
  onToggle: () => void;
}) {
  const [answer, setAnswer] = useState("");
  const [question, setQuestion] = useState(gap.question);
  const resolve = useResolveGap();
  const dismiss = useDismissGap();

  const resolved = gap.status === "RESOLVED";

  return (
    <Card>
      <button
        type="button"
        aria-expanded={open}
        onClick={onToggle}
        className="flex w-full items-center gap-3 px-4.5 py-3.5 text-left"
      >
        <StatusBadge
          tone={gap.reason === "ANSWER_FAILED" ? "error" : "warn"}
          label={GAP_REASON_LABEL[gap.reason]}
        />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-[13.5px] font-medium">{gap.question}</span>
          <span className="mt-0.5 block text-[11.5px] text-slate-2">
            최근 {dateTime(gap.lastAskedAt)}
            {gap.lastPath ? ` · ${gap.lastPath} 에서` : ""}
          </span>
        </span>
        <span className="tabular shrink-0 text-[12px] text-slate">{gap.occurrenceCount}회</span>
      </button>

      {open ? (
        <CardBody className="border-t border-line-2">
          {gap.botAnswer ? (
            <p className="mb-3.5 rounded-[7px] bg-paper px-3 py-2.5 text-[12.5px] text-slate">
              챗봇 답변 — {gap.botAnswer}
            </p>
          ) : null}

          {resolved ? (
            <Notice tone="info">
              이미 답변이 등록되어 공통 질문에 저장되었습니다. 문구를 다듬으려면 공통 질문 화면에서
              수정하세요.
            </Notice>
          ) : !editable ? (
            <Notice tone="info">보기 전용 권한입니다. 답변 등록은 편집 권한이 필요합니다.</Notice>
          ) : (
            <>
              <label htmlFor={`q-${gap.id}`} className="mb-1.5 block text-[12.5px] font-medium">
                버튼에 보일 문구
              </label>
              <input
                id={`q-${gap.id}`}
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                className={controlClass("md", "mb-3")}
              />

              <label htmlFor={`a-${gap.id}`} className="mb-1.5 block text-[12.5px] font-medium">
                이렇게 답하도록 등록
              </label>
              <textarea
                id={`a-${gap.id}`}
                rows={4}
                value={answer}
                onChange={(event) => setAnswer(event.target.value)}
                placeholder="예: 제주 및 도서 지역도 배송 가능합니다. 지역에 따라 도선료가 추가됩니다."
                className={controlClass("md", "resize-y leading-relaxed")}
              />
              <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">
                공통 질문으로 저장됩니다. 버튼 노출은 꺼진 상태로 시작하지만, 비슷한 질문이 들어오면
                바로 이 답변이 쓰입니다.
              </p>

              {resolve.isError ? (
                <Notice tone="error" className="mt-3">
                  등록하지 못했습니다. 잠시 후 다시 시도해 주세요.
                </Notice>
              ) : null}

              <div className="mt-3.5 flex flex-wrap gap-2">
                <Button
                  variant="accent"
                  size="sm"
                  disabled={answer.trim().length === 0 || resolve.isPending}
                  onClick={() =>
                    resolve.mutate({ id: gap.id, answer: answer.trim(), question: question.trim() })
                  }
                >
                  {resolve.isPending ? "등록 중…" : "답변 등록"}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={dismiss.isPending}
                  onClick={() => dismiss.mutate(gap.id)}
                >
                  넘어가기
                </Button>
              </div>
              <p className="mt-2 text-[11.5px] text-slate-2">
                넘어가도 같은 질문이 다시 들어오면 목록에 되살아납니다.
              </p>
            </>
          )}
        </CardBody>
      ) : null}
    </Card>
  );
}
