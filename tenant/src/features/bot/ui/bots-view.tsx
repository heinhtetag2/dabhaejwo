"use client";

import Link from "next/link";

import { useAppContextQuery } from "@/entities/auth/session";
import { useBotListQuery } from "@/entities/tenant/bot";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { ROUTES, botRoute } from "@/shared/config/routes";
import { canManageBilling } from "@/shared/lib/auth-store";

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
              <li key={bot.id} className="flex items-center gap-3 py-3">
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
                  {bot.lastCalledAt ? "작동 중" : "설치 확인 안 됨"}
                </span>
              </li>
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
