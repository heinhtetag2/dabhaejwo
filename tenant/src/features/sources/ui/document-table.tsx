"use client";

import {
  DOCUMENT_STATUS_LABEL,
  describeError,
  type DocumentStatus,
  type KnowledgeDocument,
} from "@/entities/chatbot/knowledge";
import { Button } from "@/shared/common/button";
import { StatusBadge, type Tone } from "@/shared/ui/status-badge";
import { date } from "@/shared/lib/format";

const TONE: Record<DocumentStatus, Tone> = {
  INDEXED: "ok",
  PROCESSING: "warn",
  PENDING: "warn",
  FAILED: "error",
  EXCLUDED: "idle",
};

export function DocumentTable({
  documents,
  editable,
  pendingId,
  onToggleExcluded,
  onDelete,
}: {
  documents: KnowledgeDocument[];
  editable: boolean;
  pendingId: string | null;
  onToggleExcluded: (document: KnowledgeDocument) => void;
  /** 업로드 문서만 지울 수 있다. 없으면 삭제 버튼을 그리지 않는다. */
  onDelete?: (document: KnowledgeDocument) => void;
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <Th>제목 · 주소</Th>
            <Th className="w-[130px]">상태</Th>
            <Th className="w-[110px]">학습 시각</Th>
            <Th className="w-[150px]" />
          </tr>
        </thead>
        <tbody>
          {documents.map((document) => (
            <tr key={document.id} className="hover:bg-paper/60">
              <td className="border-b border-line-2 px-3.5 py-3 text-[13.5px]">
                <span className="block truncate">{document.title}</span>
                {document.path ? (
                  <span className="mt-0.5 block truncate font-mono text-[11.5px] text-slate-2">
                    {document.path}
                  </span>
                ) : null}
                {document.errorCode ? (
                  <span className="mt-1 block text-[11.5px] text-brick">
                    {describeError(document.errorCode)}
                  </span>
                ) : null}
              </td>
              <td className="border-b border-line-2 px-3.5 py-3">
                <StatusBadge
                  tone={TONE[document.status]}
                  label={DOCUMENT_STATUS_LABEL[document.status]}
                />
              </td>
              <td className="border-b border-line-2 px-3.5 py-3 font-mono text-[11.5px] text-slate-2">
                {date(document.indexedAt)}
              </td>
              <td className="border-b border-line-2 px-3.5 py-3">
                {editable ? (
                  <span className="flex justify-end gap-1.5">
                    <Button
                      size="sm"
                      disabled={pendingId === document.id}
                      onClick={() => onToggleExcluded(document)}
                    >
                      {document.status === "EXCLUDED" ? "다시 포함" : "제외"}
                    </Button>
                    {/* 원본 파일이 있는 문서만 지운다. 웹페이지는 소스를 다시 읽으면 살아난다 */}
                    {onDelete && document.storageKey ? (
                      <Button
                        size="sm"
                        variant="danger"
                        disabled={pendingId === document.id}
                        onClick={() => onDelete(document)}
                      >
                        삭제
                      </Button>
                    ) : null}
                  </span>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Th({ className, children }: { className?: string; children?: React.ReactNode }) {
  return (
    <th
      className={`border-b border-line-2 px-3.5 pb-2.5 text-left font-mono text-[10.5px] font-medium tracking-[0.09em] text-slate-2 uppercase ${className ?? ""}`}
    >
      {children}
    </th>
  );
}
