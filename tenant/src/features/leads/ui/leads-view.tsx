"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  LEAD_STATUS_LABEL,
  useChangeLeadStatus,
  useLeadsQuery,
  type LeadStatus,
} from "@/entities/ops/lead";
import { env } from "@/shared/config/env";
import { Button } from "@/shared/common/button";
import { Card, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { canEdit, currentAccessToken } from "@/shared/lib/auth-store";
import { Notice } from "@/shared/ui/notice";
import { Pagination } from "@/shared/ui/pagination";
import { StatusBadge, type Tone } from "@/shared/ui/status-badge";

const TONE: Record<LeadStatus, Tone> = {
  NEW: "warn",
  CONTACTED: "ok",
  CLOSED: "idle",
};

const NEXT: Record<LeadStatus, LeadStatus> = {
  NEW: "CONTACTED",
  CONTACTED: "CLOSED",
  CLOSED: "CLOSED",
};

/**
 * 남긴 연락처. 업체 입장에서 이 화면이 곧 매출이다 (tenant-plan.md §4.4).
 *
 * <p>연락처는 <b>마스킹된 값만</b> 화면에 나온다. 원문은 CSV 내보내기에서만 나가며
 * 편집 권한이 필요하다.
 */
export function LeadsView() {
  const { data: context } = useAppContextQuery();
  const [page, setPage] = useState(0);
  const [exportError, setExportError] = useState<string | null>(null);

  const { data, isPending, isError, refetch } = useLeadsQuery(page);
  const changeStatus = useChangeLeadStatus();
  const editable = canEdit(context?.member?.role);

  const exportCsv = async () => {
    setExportError(null);
    try {
      const response = await fetch(`${env.apiBaseUrl}/api/app/leads/export`, {
        headers: { Authorization: `Bearer ${currentAccessToken() ?? ""}` },
      });
      if (!response.ok) {
        setExportError("내보내기에 실패했습니다. 편집 권한이 있는지 확인하세요.");
        return;
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "leads.csv";
      anchor.click();
      URL.revokeObjectURL(url);
    } catch {
      setExportError("내보내기에 실패했습니다.");
    }
  };

  return (
    <Card>
      <CardHeader
        title="남긴 연락처"
        aside={
          <>
            <Eyebrow>{data ? `${data.page.totalElements}명` : ""}</Eyebrow>
            {editable ? (
              <Button size="sm" onClick={() => void exportCsv()}>
                CSV 내보내기
              </Button>
            ) : null}
          </>
        }
      />

      {exportError ? (
        <Notice tone="error" className="mx-4.5 mt-3.5">
          {exportError}
        </Notice>
      ) : null}

      {isPending ? (
        <LoadingState />
      ) : isError ? (
        <ErrorState message="연락처를 불러오지 못했습니다" onRetry={() => void refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState message="아직 남긴 연락처가 없습니다. 챗봇이 답하지 못했을 때 연락처를 받도록 설정할 수 있습니다." />
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr>
                  <Th>이름</Th>
                  <Th className="w-[150px]">연락처</Th>
                  <Th>남긴 이유</Th>
                  <Th className="w-[110px]">시각</Th>
                  <Th className="w-[150px]">처리</Th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((lead) => (
                  <tr key={lead.id} className="hover:bg-paper/60">
                    <td className="border-b border-line-2 px-3.5 py-3 text-[13.5px] font-medium">
                      {lead.name}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3 font-mono text-[12px] text-slate">
                      {lead.contact}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3 text-[13px]">
                      {lead.reason ?? "—"}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3 font-mono text-[11.5px] text-slate-2">
                      {lead.createdAt.slice(0, 10)}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3">
                      <span className="flex items-center gap-2">
                        <StatusBadge tone={TONE[lead.status]} label={LEAD_STATUS_LABEL[lead.status]} />
                        {editable && lead.status !== "CLOSED" ? (
                          <Button
                            size="sm"
                            disabled={changeStatus.isPending}
                            onClick={() =>
                              changeStatus.mutate({ id: lead.id, status: NEXT[lead.status] })
                            }
                          >
                            {LEAD_STATUS_LABEL[NEXT[lead.status]]}
                          </Button>
                        ) : null}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={data.page.number}
            totalPages={data.page.totalPages}
            totalElements={data.page.totalElements}
            onChange={setPage}
          />
          <p className="px-4.5 pb-4 text-[11.5px] text-slate-2">
            화면에는 연락처를 가려서 보여줍니다. 전체 번호는 CSV 내보내기로만 확인할 수 있습니다.
          </p>
        </>
      )}
    </Card>
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
