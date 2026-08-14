"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
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
import { ROUTES } from "@/shared/config/routes";
import { errorMessage } from "@/shared/lib/error-message";
import { count, date, dateTime, quota, relative, won } from "@/shared/lib/format";
import { TenantStatusBadge } from "@/shared/ui/tenant-status-badge";
import { PageHeader } from "@/widgets/page-header/page-header";

import { TenantActions } from "./tenant-actions";
import { useTenantBotsQuery } from "@/entities/tenant/query";

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

/**
 * 업체 상세.
 *
 * <p>목록 옆의 좁은 칸이 아니라 <b>한 페이지 전부</b>를 쓴다. 담을 것이 늘면서
 * 348px 안에 요약·조치·메모·활동 이력을 다 밀어넣게 되어 어느 것도 제대로 안 보였다.
 *
 * <p>돌아가기는 {@code ?back=} 에 담아온 목록 상태를 그대로 복원한다 —
 * 여러 업체를 연속으로 확인하는 CS 흐름이 끊기지 않아야 한다.
 */
export function TenantDetailView({ tenantId }: { tenantId: string }) {
  const searchParams = useSearchParams();
  const back = searchParams.get("back");
  const listHref = back ? `${ROUTES.tenants}?${back}` : ROUTES.tenants;

  const { data: tenant, isPending, isError, error, refetch } = useTenantDetailQuery(tenantId);

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
    <>
      <div className="mb-3">
        <Link href={listHref} className="text-[12.5px] text-slate hover:text-ink">
          ← 업체 목록
        </Link>
      </div>

      <PageHeader
        title={tenant.name}
        description={`${tenant.primaryDomain} · ${tenant.publishableKey}`}
        actions={<TenantStatusBadge status={tenant.status} />}
      />

      <div className="grid items-start gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <div className="min-w-0 space-y-4">
          <Card>
            <CardHeader title="계정" />
            <CardBody>
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
            </CardBody>
          </Card>

          <BotsCard tenantId={tenant.id} />

          <Card>
            <CardHeader title="조치" />
            <CardBody>
              {churned ? (
                <p className="text-[12.5px] text-slate-2">
                  해지된 업체입니다. 읽기 전용이며 조치할 수 없습니다.
                </p>
              ) : (
                <TenantActions tenant={tenant} />
              )}
            </CardBody>
          </Card>
        </div>

        <div className="min-w-0 space-y-4">
          <NotesCard tenantId={tenant.id} readOnly={churned} />
          <ActivitiesCard tenantId={tenant.id} />
        </div>
      </div>
    </>
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
                onClick={() => mutation.mutate({ body }, { onSuccess: () => setBody("") })}
              >
                메모 저장
              </Button>
              {mutation.isError ? (
                <span className="text-[12px] text-brick">{errorMessage(mutation.error)}</span>
              ) : null}
            </div>
          </>
        ) : null}

        {/*
         * 메모도 쌓이면 페이지가 한없이 길어진다. 활동 이력과 같은 이유로 높이를 준다.
         * 다만 메모는 최근 것이 위라 잘려도 맥락을 잃지 않는다.
         */}
        <ul className="mt-4 max-h-[280px] space-y-3 overflow-y-auto pr-1">
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
  const total = data?.content.length ?? 0;

  return (
    <Card>
      <CardHeader
        title="활동 이력"
        aside={total > 0 ? <Eyebrow>{count(total)}건</Eyebrow> : null}
      />
      <CardBody>
        {/*
         * 높이를 못 박고 안에서만 스크롤한다. 안 그러면 활동이 많은 업체에서 이 카드가
         * 페이지를 수십 화면 길이로 늘려, 옆 칸의 조치 버튼이 화면 밖으로 나간다.
         */}
        <ul className="max-h-[420px] space-y-3 overflow-y-auto pr-1">
          {(data?.content ?? []).map((activity) => (
            <li
              key={activity.id}
              className="border-t border-line-2 pt-3 first:border-t-0 first:pt-0"
            >
              <div className="flex items-baseline justify-between gap-3">
                <span className="text-[13px] font-medium">
                  {ACTIVITY_LABEL[activity.type] ?? activity.type}
                </span>
                <span className="tabular shrink-0 text-[11.5px] text-slate-2">
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

function Row({ label, value, emphasis }: { label: string; value: string; emphasis?: boolean }) {
  return (
    <div className="flex justify-between gap-3 border-b border-line-2 py-2 text-[13px] last:border-b-0">
      <span className="text-slate">{label}</span>
      <span className={`tabular ${emphasis ? "font-semibold text-brick" : ""}`}>{value}</span>
    </div>
  );
}

/**
 * 업체가 가진 서비스.
 *
 * <p><b>운영 콘솔이 허용 도메인을 처음 보게 되는 자리다.</b> CS 문의 1순위가
 * "코드 붙였는데 안 떠요"이고 원인 대부분이 미등록 주소인데, 지금까지는 그걸 보려면
 * 사유를 적고 대리 로그인을 해야 했다 — 도메인 목록 하나 보려고 감사 기록을 남기는 절차를 밟았다.
 *
 * <p>탭을 만들지 않고 카드를 얹은 이유는 [대리 로그인] 바로 위에 두기 위해서다 —
 * 누르기 직전에 어느 서비스로 들어갈지 본다.
 */
function BotsCard({ tenantId }: { tenantId: string }) {
  const { data, isPending, isError } = useTenantBotsQuery(tenantId);

  return (
    <Card>
      <CardHeader
        title="서비스"
        aside={<Eyebrow>{data ? `${data.length}개` : "—"}</Eyebrow>}
      />
      <CardBody>
        {isPending ? (
          <LoadingState />
        ) : isError ? (
          <ErrorState message="서비스를 불러오지 못했습니다" />
        ) : data.length === 0 ? (
          <EmptyState message="서비스가 없습니다." />
        ) : (
          <ul className="divide-y divide-line">
            {data.map((bot) => (
              <li key={bot.id} className="py-3">
                <div className="flex items-baseline gap-2">
                  <span className="min-w-0 flex-1 truncate text-[13.5px] font-medium">
                    {bot.name}
                  </span>
                  {/* 색만으로 구분하지 않는다 — 글자로도 말한다 (WCAG 2.1 AA) */}
                  <span className="shrink-0 text-[11.5px] text-slate-2">
                    {bot.lastCalledAt ? "작동 중" : "설치 확인 안 됨"}
                  </span>
                </div>
                <div className="truncate font-mono text-[11px] text-slate-2">
                  {bot.primaryDomain} · {bot.publishableKey}
                </div>
                <ul className="mt-1.5 flex flex-wrap gap-x-3 gap-y-1">
                  {bot.allowedOrigins.map((origin) => (
                    <li key={origin.id} className="font-mono text-[11px] text-slate-2">
                      {origin.origin}
                      <span className="ml-1 font-sans">
                        {origin.lastCalledAt ? "✔" : "· 호출 없음"}
                      </span>
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ul>
        )}
      </CardBody>
    </Card>
  );
}
