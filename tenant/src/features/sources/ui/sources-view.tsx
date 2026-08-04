"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  SOURCE_TYPE_LABEL,
  useChangeAutoRefresh,
  useChangeExcluded,
  useKnowledgeDocumentsQuery,
  useKnowledgeSourcesQuery,
  useDeleteDocument,
  useRecrawlSource,
  useRetryFailed,
  type DocumentStatus,
  type KnowledgeDocument,
} from "@/entities/chatbot/knowledge";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { canEdit } from "@/shared/lib/auth-store";
import { cn } from "@/shared/lib/cn";
import { Notice } from "@/shared/ui/notice";
import { Pagination } from "@/shared/ui/pagination";

import { DocumentTable } from "./document-table";
import { FileUpload } from "./file-upload";

const STATUS_FILTERS: Array<{ value: DocumentStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "전체" },
  { value: "INDEXED", label: "학습 완료" },
  { value: "PROCESSING", label: "처리 중" },
  { value: "FAILED", label: "실패" },
  { value: "EXCLUDED", label: "제외됨" },
];

/**
 * 지식 소스.
 *
 * <p>전부 자동으로 학습하지 않고 <b>고르게 하는 것</b>이 중요하다. 채용 공고나 이용약관처럼
 * 챗봇이 답할 필요 없는 페이지가 섞이면 답변 품질이 떨어진다 (tenant-plan.md §4.5).
 */
export function SourcesView() {
  const { data: context } = useAppContextQuery();
  const { data: sources, isPending, isError, refetch } = useKnowledgeSourcesQuery();

  const [sourceId, setSourceId] = useState<string | undefined>();
  const [status, setStatus] = useState<DocumentStatus | "ALL">("ALL");
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [notice, setNotice] = useState<string | null>(null);

  const editable = canEdit(context?.member?.role);

  // 고른 적이 없으면 첫 소스를 본다. 상태로 복사해 두면 목록이 바뀔 때 어긋나므로 파생값으로 둔다.
  const effectiveSourceId = sourceId ?? sources?.[0]?.id;

  const documents = useKnowledgeDocumentsQuery({
    sourceId: effectiveSourceId,
    status: status === "ALL" ? undefined : status,
    q: query || undefined,
    page,
  });

  const autoRefresh = useChangeAutoRefresh();
  const excluded = useChangeExcluded();
  const recrawl = useRecrawlSource();
  const retry = useRetryFailed();
  const removeDocument = useDeleteDocument();

  if (isPending) {
    return <LoadingState />;
  }
  if (isError) {
    return <ErrorState message="지식 소스를 불러오지 못했습니다" onRetry={() => void refetch()} />;
  }
  if (sources.length === 0) {
    return (
      <EmptyState message="등록된 지식 소스가 없습니다. 사이트 주소나 파일을 등록하면 챗봇이 학습합니다." />
    );
  }

  const selected = sources.find((source) => source.id === effectiveSourceId) ?? sources[0];

  const runStub = (action: () => Promise<unknown>) => {
    setNotice(null);
    void action().catch((error: unknown) => {
      setNotice(
        error instanceof ApiError ? error.message : "요청을 처리하지 못했습니다",
      );
    });
  };

  const handleToggleExcluded = (document: KnowledgeDocument) => {
    excluded.mutate({ id: document.id, excluded: document.status !== "EXCLUDED" });
  };

  return (
    <>
      <div role="tablist" aria-label="지식 소스 종류" className="mb-4 flex flex-wrap gap-1">
        {sources.map((source) => (
          <button
            key={source.id}
            role="tab"
            aria-selected={source.id === selected.id}
            onClick={() => {
              setSourceId(source.id);
              setPage(0);
              setNotice(null);
            }}
            className={cn(
              "rounded-[7px] px-3 py-1.5 text-[13px] transition-colors",
              source.id === selected.id
                ? "bg-ink text-white"
                : "border border-line bg-card hover:bg-line-2/60",
            )}
          >
            {SOURCE_TYPE_LABEL[source.type]}{" "}
            <span className="tabular ml-1 text-[11.5px] opacity-70">{source.documentCount}</span>
          </button>
        ))}
      </div>

      <Card className="mb-4">
        <CardBody className="flex flex-wrap items-center gap-3.5">
          <div className="min-w-[200px] flex-1">
            <p className="text-[13.5px] font-medium">{selected.origin}</p>
            <p className="mt-0.5 text-[11.5px] text-slate-2">
              {selected.type === "WEBSITE"
                ? selected.autoRefresh
                  ? "매주 자동으로 다시 읽습니다"
                  : "자동 갱신이 꺼져 있습니다"
                : "직접 등록한 자료입니다"}
              {selected.lastCrawledAt
                ? ` · 마지막 ${selected.lastCrawledAt.slice(0, 10)}`
                : " · 아직 읽은 적 없음"}
            </p>
          </div>

          {selected.type === "WEBSITE" ? (
            <label className="flex items-center gap-2 text-[12.5px] text-slate">
              <input
                type="checkbox"
                checked={selected.autoRefresh}
                disabled={!editable || autoRefresh.isPending}
                onChange={(event) =>
                  autoRefresh.mutate({ id: selected.id, autoRefresh: event.target.checked })
                }
              />
              자동 갱신
            </label>
          ) : null}

          {editable && selected.type === "WEBSITE" ? (
            <Button
              size="sm"
              disabled={recrawl.isPending}
              onClick={() => runStub(() => recrawl.mutateAsync(selected.id))}
            >
              지금 다시 읽기
            </Button>
          ) : null}
        </CardBody>
      </Card>

      {editable && selected.type === "FILE" ? (
        <div className="mb-4">
          <FileUpload onUploaded={() => setPage(0)} />
        </div>
      ) : null}

      {notice ? (
        <Notice tone="warn" className="mb-4">
          {notice}
        </Notice>
      ) : null}

      <Card>
        <CardHeader
          title="수집된 문서"
          aside={
            <>
              <label className="sr-only" htmlFor="doc-search">
                주소나 제목으로 찾기
              </label>
              <input
                id="doc-search"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    setQuery(searchInput.trim());
                    setPage(0);
                  }
                }}
                placeholder="주소나 제목으로 찾기"
                className="w-[190px] rounded-[7px] border border-line px-2.5 py-[5.5px] text-[12.5px] focus:border-ink-3 focus:outline-none"
              />
              <Button
                size="sm"
                onClick={() => {
                  setQuery(searchInput.trim());
                  setPage(0);
                }}
              >
                찾기
              </Button>
            </>
          }
        />

        <div className="flex flex-wrap gap-1 border-b border-line-2 px-3.5 py-2.5">
          {STATUS_FILTERS.map((filter) => (
            <button
              key={filter.value}
              aria-pressed={status === filter.value}
              onClick={() => {
                setStatus(filter.value);
                setPage(0);
              }}
              className={cn(
                "rounded-full px-2.5 py-1 text-[12px] transition-colors",
                status === filter.value
                  ? "bg-mark-soft text-[#8a6a00]"
                  : "text-slate hover:bg-line-2",
              )}
            >
              {filter.label}
            </button>
          ))}
          {editable ? (
            <Button
              size="sm"
              className="ml-auto"
              disabled={retry.isPending}
              onClick={() => runStub(() => retry.mutateAsync(selected.id))}
            >
              실패분 다시 학습
            </Button>
          ) : null}
        </div>

        {documents.isPending ? (
          <LoadingState />
        ) : documents.isError ? (
          <ErrorState message="문서를 불러오지 못했습니다" onRetry={() => void documents.refetch()} />
        ) : documents.data.content.length === 0 ? (
          <EmptyState
            message={
              query || status !== "ALL"
                ? "조건에 맞는 문서가 없습니다."
                : "아직 수집된 문서가 없습니다."
            }
            action={
              query || status !== "ALL" ? (
                <Button
                  size="sm"
                  onClick={() => {
                    setQuery("");
                    setSearchInput("");
                    setStatus("ALL");
                    setPage(0);
                  }}
                >
                  필터 해제
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <DocumentTable
              documents={documents.data.content}
              editable={editable}
              pendingId={
                excluded.isPending
                  ? excluded.variables?.id ?? null
                  : removeDocument.isPending
                    ? removeDocument.variables ?? null
                    : null
              }
              onToggleExcluded={handleToggleExcluded}
              onDelete={
                editable
                  ? (document) => runStub(() => removeDocument.mutateAsync(document.id))
                  : undefined
              }
            />
            <Pagination
              page={documents.data.page.number}
              totalPages={documents.data.page.totalPages}
              totalElements={documents.data.page.totalElements}
              onChange={setPage}
            />
          </>
        )}
      </Card>

      <Eyebrow className="mt-4 block">
        제외한 문서는 지워지지 않습니다. 학습 대상에서만 빠지고 요금제 한도에도 잡히지 않습니다.
      </Eyebrow>
    </>
  );
}
