"use client";

import Link from "next/link";
import { useState } from "react";

import {
  useConversationDetailQuery,
  useConversationsQuery,
  type ConversationMessage,
} from "@/entities/ops/conversation";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { ROUTES } from "@/shared/config/routes";
import { cn } from "@/shared/lib/cn";
import { Pagination } from "@/shared/ui/pagination";
import { StatusBadge } from "@/shared/ui/status-badge";
import { controlClass } from "@/shared/common/control";

/**
 * 대화 로그. 좌측 목록 + 우측 상세.
 *
 * <p>답을 못 한 말풍선은 배경을 달리하고 "여기에 답 달기"를 붙인다 — 대화를 읽다가
 * 바로 개선으로 넘어갈 수 있어야 개선 루프가 끊기지 않는다 (tenant-plan.md §4.3).
 */
export function ConversationsView() {
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const list = useConversationsQuery(query, page);
  const effectiveId = selectedId ?? list.data?.content[0]?.id ?? null;
  const detail = useConversationDetailQuery(effectiveId);

  return (
    <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
      <Card className="self-start">
        <CardHeader
          title={
            <span className="sr-only">대화 검색</span>
          }
          aside={
            <>
              <input
                aria-label="대화 내용 검색"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    setQuery(searchInput.trim());
                    setPage(0);
                    setSelectedId(null);
                  }
                }}
                placeholder="대화 내용 검색"
                className={controlClass("sm")}
              />
            </>
          }
        />

        {list.isPending ? (
          <LoadingState />
        ) : list.isError ? (
          <ErrorState message="대화를 불러오지 못했습니다" onRetry={() => void list.refetch()} />
        ) : list.data.content.length === 0 ? (
          <EmptyState
            message={query ? "조건에 맞는 대화가 없습니다." : "아직 대화가 없습니다."}
            action={
              query ? (
                <Button
                  size="sm"
                  onClick={() => {
                    setQuery("");
                    setSearchInput("");
                  }}
                >
                  검색 해제
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <ul className="max-h-[600px] overflow-y-auto">
              {list.data.content.map((conversation) => (
                <li key={conversation.id}>
                  <button
                    type="button"
                    onClick={() => setSelectedId(conversation.id)}
                    aria-current={conversation.id === effectiveId ? "true" : undefined}
                    className={cn(
                      "w-full border-b border-line-2 px-3.5 py-2.5 text-left transition-colors",
                      conversation.id === effectiveId ? "bg-mark-soft/40" : "hover:bg-paper/60",
                    )}
                  >
                    <span className="flex items-center gap-2">
                      <span className="min-w-0 flex-1 truncate text-[12.5px] font-medium">
                        방문자{conversation.visitorRegion ? ` · ${conversation.visitorRegion}` : ""}
                      </span>
                      <span className="tabular shrink-0 text-[11px] text-slate-2">
                        {conversation.startedAt.slice(11, 16)}
                      </span>
                    </span>
                    <span className="mt-1 flex items-center gap-1.5">
                      <span className="min-w-0 flex-1 truncate text-[12px] text-slate">
                        {conversation.preview ?? "메시지 없음"}
                      </span>
                      {conversation.hasFailure ? (
                        <StatusBadge tone="error" label="실패" />
                      ) : null}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            <Pagination
              page={list.data.page.number}
              totalPages={list.data.page.totalPages}
              totalElements={list.data.page.totalElements}
              onChange={(next) => {
                setPage(next);
                setSelectedId(null);
              }}
            />
          </>
        )}
      </Card>

      <Card>
        {effectiveId === null ? (
          <EmptyState message="왼쪽에서 대화를 고르세요." />
        ) : detail.isPending ? (
          <LoadingState />
        ) : detail.isError ? (
          <ErrorState message="대화를 불러오지 못했습니다" onRetry={() => void detail.refetch()} />
        ) : (
          <>
            <CardHeader
              title={`방문자${detail.data.visitorRegion ? ` · ${detail.data.visitorRegion}` : ""}`}
              aside={
                <span className="text-[11.5px] text-slate-2">
                  {detail.data.startedAt.slice(0, 16).replace("T", " ")}
                  {detail.data.startedPath ? ` · ${detail.data.startedPath} 에서 시작` : ""}
                </span>
              }
            />
            <CardBody className="space-y-3">
              {detail.data.messages.map((message) => (
                <Bubble key={message.id} message={message} />
              ))}
            </CardBody>
          </>
        )}
      </Card>
    </div>
  );
}

function Bubble({ message }: { message: ConversationMessage }) {
  if (message.role === "VISITOR") {
    return (
      <p className="ml-auto w-fit max-w-[80%] rounded-[12px] bg-ink px-3.5 py-2.5 text-[13px] text-white">
        {message.content}
      </p>
    );
  }

  const failed = message.answered === false;

  return (
    <div
      className={cn(
        "w-fit max-w-[85%] rounded-[12px] border px-3.5 py-2.5 text-[13px] whitespace-pre-line",
        failed ? "border-[#ebbfb7] bg-[#fef7f5]" : "border-line-2 bg-paper",
      )}
    >
      {message.content}
      {message.saved ? (
        <span className="mt-1.5 block font-mono text-[10.5px] text-slate-2">
          저장된 답변 · 사용량에 포함되지 않음
        </span>
      ) : null}
      {failed ? (
        <span className="mt-2.5 flex flex-wrap items-center gap-2 border-t border-dashed border-[#ebbfb7] pt-2.5">
          <StatusBadge tone="error" label="답변 실패" />
          <Link
            href={ROUTES.improve}
            className="rounded-md bg-mark px-2.5 py-1 text-[12px] font-medium text-ink"
          >
            여기에 답 달기
          </Link>
        </span>
      ) : null}
    </div>
  );
}
