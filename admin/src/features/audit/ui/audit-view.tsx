"use client";

import { useState } from "react";

import { useAuditLogListQuery, type AuditAction } from "@/entities/audit";
import { Badge } from "@/shared/common/badge";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { Select } from "@/shared/common/field";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th } from "@/shared/common/table";
import { errorMessage } from "@/shared/lib/error-message";
import { dateTime } from "@/shared/lib/format";
import { PageHeader } from "@/widgets/page-header/page-header";

const ACTION_LABEL: Record<AuditAction, string> = {
  IMPERSONATE: "대리 로그인",
  VIEW_CONVERSATIONS: "대화 로그 열람",
  CHANGE_PLAN: "요금제 변경",
  GRANT_QUOTA: "쿼터 증량",
  SUSPEND: "일시정지",
  CHURN: "해지",
  EXTEND_TRIAL: "체험 연장",
  ACTIVATE: "정지 해제",
  MODEL_PRICE_WRITE: "단가 등록",
  COST_GUARD_WRITE: "안전장치 수정",
  PLAN_WRITE: "요금제 정의",
  FLAG_WRITE: "기능 공개",
  TICKET_WRITE: "문의 처리",
};

/** 고객 데이터를 직접 보는 행위는 눈에 띄게 구분한다. */
const CUSTOMER_DATA_ACTIONS: AuditAction[] = ["IMPERSONATE", "VIEW_CONVERSATIONS"];

export function AuditView() {
  const [action, setAction] = useState<AuditAction | "">("");
  const { data, isPending, isError, error, refetch } = useAuditLogListQuery({
    action: action || undefined,
  });

  return (
    <>
      <PageHeader title="감사 기록" description="운영자가 고객 데이터에 접근한 이력 · 최근 30일" />

      <Card>
        <CardHeader
          title="감사 기록"
          aside={
            <label className="flex items-center gap-2">
              <Eyebrow>행위</Eyebrow>
              <Select
                value={action}
                onChange={(e) => setAction(e.target.value as AuditAction | "")}
                className="w-auto px-2 py-1 text-[12.5px]"
              >
                <option value="">전체</option>
                {Object.entries(ACTION_LABEL).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </Select>
            </label>
          }
        />

        {isPending ? <LoadingState /> : null}
        {isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : null}
        {data && data.content.length === 0 ? (
          <EmptyState message="해당 기간에 기록이 없습니다" />
        ) : null}

        {data && data.content.length > 0 ? (
          <Table>
            <thead>
              <tr>
                <Th className="w-[140px]">시각</Th>
                <Th className="w-[90px]">운영자</Th>
                <Th className="w-[120px]">행위</Th>
                <Th className="w-[140px]">대상</Th>
                <Th>사유</Th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((log) => (
                <tr key={log.id}>
                  <Td className="tabular text-[12.5px] text-slate-2">{dateTime(log.at)}</Td>
                  <Td className="text-[12.5px] text-slate">{log.operator.name}</Td>
                  <Td>
                    <Badge
                      tone={CUSTOMER_DATA_ACTIONS.includes(log.action) ? "info" : "idle"}
                      dot={false}
                    >
                      {ACTION_LABEL[log.action] ?? log.action}
                    </Badge>
                  </Td>
                  <Td className="font-medium">{log.tenant?.name ?? "—"}</Td>
                  <Td className="text-slate">{log.reason || "—"}</Td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : null}
      </Card>

      <Card className="mt-4">
        <CardBody className="text-[13px] leading-[1.7] text-slate">
          대리 로그인과 대화 로그 열람은 고객 데이터를 직접 보는 행위입니다. 사유 없이는 실행되지
          않으며, <b className="font-semibold text-ink">기록은 지울 수 없습니다</b> — 앱이 아니라
          DB 트리거가 수정·삭제를 막습니다. 업체 대시보드에서도 자기 계정에 대한 운영팀 접속
          이력을 볼 수 있습니다.
        </CardBody>
      </Card>
    </>
  );
}
