"use client";

import Link from "next/link";
import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import { useBotListQuery, useDeleteBot, useRestoreBot } from "@/entities/tenant/bot";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { ROUTES, botRoute } from "@/shared/config/routes";
import { canManageBilling } from "@/shared/lib/auth-store";
import { Button } from "@/shared/common/button";
import { controlClass } from "@/shared/common/control";
import { Notice } from "@/shared/ui/notice";
import { ApiError } from "@/shared/api/http-client";
import type { Bot } from "@/entities/auth/session";

/**
 * 서비스 목록.
 *
 * <p>이름 변경·삭제를 여기서만 할 수 있게 두면 <b>보고 있는 화면을 지우는 상황이
 * 구조적으로 생기지 않는다.</b> 그래서 서비스별 설정 화면을 따로 만들지 않았다.
 */
export function BotsView() {
  const { data: context } = useAppContextQuery();
  const { data: bots, isPending, isError, refetch } = useBotListQuery();

  if (isPending) {
    return <LoadingState />;
  }
  if (isError) {
    return <ErrorState message="서비스를 불러오지 못했습니다" onRetry={() => void refetch()} />;
  }

  const usage = context?.usage;
  // 서비스 추가는 요금이 걸린 행위다 — 소유자만.
  const owner = canManageBilling(context?.member?.role);
  const atLimit = usage ? usage.botCount >= usage.botLimit : false;
  // 마지막 하나는 지울 수 없다 — 삭제 예정인 것은 살아 있는 축에서 뺀다.
  const alive = bots.filter((bot) => bot.status !== "DELETING").length;

  return (
    <Card>
      <CardHeader
        title="서비스"
        aside={
          <>
            <Eyebrow>
              {usage ? `${usage.botCount} / ${usage.botLimit}` : "—"}
            </Eyebrow>
            {owner ? (
              <Link
                href={ROUTES.botNew}
                className="rounded-[7px] bg-ink px-3 py-1.5 text-[12.5px] font-medium text-white"
              >
                서비스 추가
              </Link>
            ) : null}
          </>
        }
      />
      <CardBody>
        <p className="mb-4 text-[13px] leading-relaxed text-slate">
          서비스마다 <b className="font-semibold text-ink">지식·공통 질문·말투·대화 기록이 따로</b>{" "}
          갑니다. 요금제와 팀원은 업체 하나로 공유하고,{" "}
          <b className="font-semibold text-ink">대화·문서 한도도 서비스들이 나눠 씁니다.</b>
        </p>

        {bots.length === 0 ? (
          <EmptyState message="서비스가 없습니다." />
        ) : (
          <ul className="divide-y divide-line">
            {bots.map((bot) => (
              <BotRow key={bot.id} bot={bot} owner={owner} deletable={alive > 1} />
            ))}
          </ul>
        )}

        {atLimit ? (
          <p className="mt-4 text-[12px] leading-relaxed text-slate-2">
            요금제에 포함된 서비스를 모두 쓰고 있습니다.{" "}
            <Link href={ROUTES.plan} className="underline">
              요금제 보기
            </Link>
          </p>
        ) : null}
      </CardBody>
    </Card>
  );
}

/**
 * 서비스 한 줄.
 *
 * <p><b>삭제를 목록에서만 할 수 있게 두면 "보고 있는 화면을 지우는" 상황이 구조적으로
 * 생기지 않는다.</b> 그래서 서비스별 설정 화면을 따로 만들지 않았다.
 */
function BotRow({
  bot,
  owner,
  deletable,
}: {
  bot: Bot;
  owner: boolean;
  deletable: boolean;
}) {
  const [confirming, setConfirming] = useState(false);
  const [typed, setTyped] = useState("");
  const remove = useDeleteBot();
  const restore = useRestoreBot();
  const deleting = bot.status === "DELETING";

  return (
    <li className="py-3">
      <div className="flex items-center gap-3">
        <span className="min-w-0 flex-1">
          <Link href={botRoute(bot.id)} className="block truncate text-[14px] font-medium">
            {bot.name}
          </Link>
          <span className="block truncate font-mono text-[11.5px] text-slate-2">
            {bot.primaryDomain} · {bot.publishableKey}
          </span>
        </span>
        {/* 색만으로 구분하지 않는다 — 글자로도 말한다 (WCAG 2.1 AA) */}
        <span className="shrink-0 text-[11.5px] text-slate-2">
          {deleting ? "삭제 예정" : bot.lastCalledAt ? "작동 중" : "설치 확인 안 됨"}
        </span>
        {owner ? (
          deleting ? (
            <Button size="sm" disabled={restore.isPending} onClick={() => restore.mutate(bot.id)}>
              되돌리기
            </Button>
          ) : deletable ? (
            <Button size="sm" variant="danger" onClick={() => setConfirming(true)}>
              삭제
            </Button>
          ) : null
        ) : null}
      </div>

      {deleting ? (
        <Notice tone="warn" className="mt-2">
          챗봇이 멈췄습니다. {remainingDays(bot.purgeAfter)} 지식·대화·연락처가 삭제됩니다 —
          그전까지는 되돌릴 수 있습니다.
        </Notice>
      ) : null}

      {confirming ? (
        <div className="mt-2 rounded-[7px] border border-brick/30 bg-brick/5 p-3.5">
          {/* 무엇이 사라지는지 적는다. 안 적으면 지운 뒤에 알게 된다. */}
          <p className="text-[12.5px] leading-relaxed">
            <b className="font-semibold">{bot.name}</b> 을(를) 지우면 챗봇이 즉시 멈추고,
            유예 기간 뒤에 <b className="font-semibold">지식 문서 · 대화 로그 · 남긴 연락처</b>가
            함께 사라집니다.
          </p>
          <p className="mt-1.5 text-[11.5px] text-slate-2">
            확인을 위해 서비스 이름을 그대로 입력해 주세요.
          </p>
          <input
            value={typed}
            onChange={(event) => setTyped(event.target.value)}
            placeholder={bot.name}
            className={controlClass("sm", "mt-2")}
          />
          <div className="mt-2.5 flex gap-2">
            <Button
              size="sm"
              variant="danger"
              disabled={typed.trim() !== bot.name || remove.isPending}
              onClick={() => remove.mutate(bot.id, { onSuccess: () => setConfirming(false) })}
            >
              {remove.isPending ? "지우는 중…" : "삭제"}
            </Button>
            <Button size="sm" onClick={() => setConfirming(false)}>
              취소
            </Button>
          </div>
          {remove.isError ? (
            <Notice tone="error" className="mt-2">
              {remove.error instanceof ApiError ? remove.error.message : "지우지 못했습니다"}
            </Notice>
          ) : null}
        </div>
      ) : null}
    </li>
  );
}

/**
 * 언제 사라지는지 말한다.
 *
 * <p><b>남은 날짜를 화면이 계산하지 않는다.</b> 유예 일수는 안전장치 설정이라
 * 운영자가 바꿀 수 있고, 화면이 상수로 들고 있으면 바꾼 날부터 둘이 어긋난다.
 * 서버가 준 시각만 읽는다.
 */
function remainingDays(purgeAfter: string | null): string {
  if (!purgeAfter) {
    return "유예 기간이 지나면";
  }
  const days = Math.ceil((new Date(purgeAfter).getTime() - Date.now()) / 86_400_000);
  return days > 0 ? `${days}일 뒤` : "곧";
}
