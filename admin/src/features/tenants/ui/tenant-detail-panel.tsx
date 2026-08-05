"use client";

import { useState } from "react";

import {
  useAddTenantNote,
  useTenantActivitiesQuery,
  useTenantDetailQuery,
  useTenantNotesQuery,
  type TenantActivityType,
} from "@/entities/tenant";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { TextArea } from "@/shared/common/field";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { errorMessage } from "@/shared/lib/error-message";
import { count, date, dateTime, quota, relative, won } from "@/shared/lib/format";
import { TenantStatusBadge } from "@/shared/ui/tenant-status-badge";

import { TenantActions } from "./tenant-actions";

const ACTIVITY_LABEL: Record<TenantActivityType, string> = {
  CHANGE_PLAN: "요금제 변경",
  GRANT_QUOTA: "쿼터 증량",
  SUSPEND: "일시정지",
  CHURN: "해지",
  EXTEND_TRIAL: "체험 연장",
  ACTIVATE: "정지 해제",
  IMPERSONATE: "대리 로그인",
  VIEW_CONVERSATIONS: "대화 로그 열람",
  MODEL_PRICE_WRITE: "단가 수정",
  COST_GUARD_WRITE: "안전장치 수정",
  PAYMENT: "결제",
  NOTE: "메모",
} as Record<TenantActivityType, string>;

export function TenantDetailPanel({ tenantId }: { tenantId: string | null }) {
  const { data: tenant, isPending, isError, error, refetch } = useTenantDetailQuery(tenantId);

  if (!tenantId) {
    return (
      <Card>
        <EmptyState message="왼쪽 목록에서 업체를 고르세요" />
      </Card>
    );
  }
  if (isPending) {
    return (
      <Card>
        <LoadingState />
      </Card>
    );
  }
  if (isError) {
    return (
      <Card>
        <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
      </Card>
    );
  }

  const churned = tenant.status === "CHURNED";

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader title={tenant.name} aside={<TenantStatusBadge status={tenant.status} />} />
        <CardBody>
          <p className="tabular mb-3.5 text-[11.5px] text-slate-2">
            {tenant.primaryDomain} · {tenant.publishableKey}
          </p>

          <Row label="가입일" value={date(tenant.joinedDate)} />
          <Row
            label="요금제"
            value={
              tenant.plan ? `${tenant.plan.name} ${won(tenant.plan.monthlyFee)}` : "요금제 없음"
            }
          />
          <Row label="이번 달 대화" value={quota(tenant.convCount, tenant.convLimit)} />
          <Row label="학습 문서" value={quota(tenant.docCount, tenant.docLimit)} />
          {/* 공통 질문 0개는 원가 급증의 선행 지표다 (admin-console-tenant-plan.md §4.2.2) */}
          <Row
            label="공통 질문"
            value={`${count(tenant.faqCount)}개`}
            emphasis={tenant.faqCount === 0}
          />
          <Row label="저장 답변 비율" value={`${tenant.savedAnswerPercent}%`} />
          <Row
            label="이번 달 모델 원가"
            value={won(Math.round(tenant.costKrw))}
            emphasis={tenant.costRatioPercent >= 100}
          />
          <Row label="다음 결제일" value={date(tenant.nextBillingDate)} />
          <Row label="마지막 접속" value={relative(tenant.lastSeenAt)} />

          <div className="mt-4 border-t border-line-2 pt-3.5">
            <Eyebrow className="mb-2 block">조치</Eyebrow>
            {churned ? (
              <p className="text-[12.5px] text-slate-2">
                해지된 업체입니다. 읽기 전용이며 조치할 수 없습니다.
              </p>
            ) : (
              <TenantActions tenant={tenant} />
            )}
          </div>
        </CardBody>
      </Card>

      <NotesCard tenantId={tenant.id} readOnly={churned} />
      <ActivitiesCard tenantId={tenant.id} />
    </div>
  );
}

/** 메모는 누적이다. 수정·삭제 경로가 없는 것이 설계다. */
function NotesCard({ tenantId, readOnly }: { tenantId: string; readOnly: boolean }) {
  const { data: notes } = useTenantNotesQuery(tenantId);
  const [body, setBody] = useState("");
  const mutation = useAddTenantNote(tenantId);

  return (
    <Card>
      <CardHeader title="내부 메모" aside={<Eyebrow>누적</Eyebrow>} />
      <CardBody>
        {!readOnly ? (
          <>
            <TextArea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="영업·CS 이력을 남겨두세요"
              aria-label="내부 메모"
              className="min-h-16"
            />
            <div className="mt-2 flex items-center gap-2">
              <Button
                size="sm"
                disabled={body.trim().length === 0 || mutation.isPending}
                onClick={() =>
                  mutation.mutate({ body }, { onSuccess: () => setBody("") })
                }
              >
                메모 저장
              </Button>
              {mutation.isError ? (
                <span className="text-[12px] text-brick">{errorMessage(mutation.error)}</span>
              ) : null}
            </div>
          </>
        ) : null}

        <ul className="mt-4 space-y-3">
          {(notes ?? []).map((note) => (
            <li key={note.id} className="border-t border-line-2 pt-3 first:border-t-0 first:pt-0">
              <p className="text-[13px] leading-relaxed whitespace-pre-wrap">{note.body}</p>
              <p className="tabular mt-1 text-[11.5px] text-slate-2">
                {note.operator?.name ?? "—"} · {dateTime(note.createdAt)}
              </p>
            </li>
          ))}
          {notes && notes.length === 0 ? (
            <li className="text-[12.5px] text-slate-2">남긴 메모가 없습니다</li>
          ) : null}
        </ul>
      </CardBody>
    </Card>
  );
}

/** 감사 기록·결제·메모를 합친 읽기 전용 뷰. 별도 테이블을 두지 않는다. */
function ActivitiesCard({ tenantId }: { tenantId: string }) {
  const { data } = useTenantActivitiesQuery(tenantId);

  return (
    <Card>
      <CardHeader title="활동 이력" />
      <CardBody>
        <ul className="space-y-3">
          {(data?.content ?? []).map((activity) => (
            <li
              key={activity.id}
              className="border-t border-line-2 pt-3 first:border-t-0 first:pt-0"
            >
              <div className="flex items-baseline justify-between gap-3">
                <span className="text-[13px] font-medium">
                  {ACTIVITY_LABEL[activity.type] ?? activity.type}
                </span>
                <span className="tabular text-[11.5px] text-slate-2">
                  {dateTime(activity.at)}
                </span>
              </div>
              <p className="mt-0.5 text-[12.5px] text-slate">{activity.summary}</p>
              {activity.reason ? (
                <p className="mt-0.5 text-[11.5px] text-slate-2">사유: {activity.reason}</p>
              ) : null}
              {activity.operator ? (
                <p className="tabular mt-0.5 text-[11.5px] text-slate-2">
                  {activity.operator.name}
                </p>
              ) : null}
            </li>
          ))}
          {data && data.content.length === 0 ? (
            <li className="text-[12.5px] text-slate-2">기록된 활동이 없습니다</li>
          ) : null}
        </ul>
      </CardBody>
    </Card>
  );
}

function Row({
  label,
  value,
  emphasis,
}: {
  label: string;
  value: string;
  emphasis?: boolean;
}) {
  return (
    <div className="flex justify-between gap-3 border-b border-line-2 py-2 text-[13px] last:border-b-0">
      <span className="text-slate">{label}</span>
      <span className={`tabular ${emphasis ? "font-semibold text-brick" : ""}`}>{value}</span>
    </div>
  );
}
