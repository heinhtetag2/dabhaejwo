"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  useBillingMethodQuery,
  useRegisterBillingMethod,
  useRemoveBillingMethod,
} from "@/entities/tenant/billing";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader } from "@/shared/common/card";
import { LoadingState } from "@/shared/common/states";
import { ROUTES } from "@/shared/config/routes";
import { env } from "@/shared/config/env";
import { canManageBilling } from "@/shared/lib/auth-store";
import { date } from "@/shared/lib/format";
import { Notice } from "@/shared/ui/notice";

/**
 * 결제수단 등록.
 *
 * <p>카드번호는 <b>이 화면을 지나지 않는다.</b> 토스 결제창이 직접 받아가고, 우리에게는
 * 일회용 인증키만 돌아온다. 그래서 카드 정보 취급에 따른 책임을 지지 않는다.
 *
 * <p>등록 성공 후 돌아오는 곳이 이 화면이라, 주소창의 {@code authKey} 를 보고 이어서 처리한다.
 */
export function BillingMethodCard() {
  const { data: context } = useAppContextQuery();
  const method = useBillingMethodQuery();
  const remove = useRemoveBillingMethod();

  const [error, setError] = useState<string | null>(null);
  const [opening, setOpening] = useState(false);

  const owner = canManageBilling(context?.member?.role);
  const configured = env.tossClientKey.length > 0;

  const openBillingWindow = async () => {
    setError(null);
    setOpening(true);
    try {
      const { loadTossPayments } = await import("@tosspayments/tosspayments-sdk");
      const toss = await loadTossPayments(env.tossClientKey);
      // customerKey 는 업체 id 다. 서버가 토큰의 업체와 대조하므로 위조해도 통과하지 않는다.
      const payment = toss.payment({ customerKey: context!.tenant.id });

      const returnTo = `${window.location.origin}${ROUTES.plan}`;
      await payment.requestBillingAuth({
        method: "CARD",
        successUrl: `${returnTo}?billing=success`,
        failUrl: `${returnTo}?billing=fail`,
        customerEmail: context?.member?.email ?? undefined,
        customerName: context?.member?.name ?? undefined,
      });
    } catch (cause) {
      // 사용자가 결제창을 닫은 것도 여기로 온다. 오류로 단정하지 않는다.
      setError(cause instanceof Error ? cause.message : "결제창을 열지 못했습니다");
    } finally {
      setOpening(false);
    }
  };

  if (method.isPending) {
    return (
      <Card className="mb-4">
        <CardHeader title="결제수단" />
        <LoadingState />
      </Card>
    );
  }

  const registered = method.data?.registered === true;

  return (
    <Card className="mb-4">
      <CardHeader title="결제수단" />
      <CardBody>
        {registered ? (
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-[13.5px] font-medium">
              {method.data?.cardCompany ?? "카드"} {method.data?.cardNumberMasked}
            </span>
            <span className="text-[12px] text-slate-2">
              {date(method.data?.registeredAt)} 등록
            </span>
            {owner ? (
              <Button
                size="sm"
                variant="danger"
                className="ml-auto"
                disabled={remove.isPending}
                onClick={() => {
                  setError(null);
                  remove.mutate(undefined, {
                    onError: (cause) =>
                      setError(cause instanceof ApiError ? cause.message : "삭제하지 못했습니다"),
                  });
                }}
              >
                카드 삭제
              </Button>
            ) : null}
          </div>
        ) : (
          <p className="text-[13px] leading-relaxed text-slate">
            등록된 카드가 없습니다. 유료 요금제는 카드를 등록해야 이용할 수 있습니다.
          </p>
        )}

        {owner && configured ? (
          <Button
            variant={registered ? "default" : "primary"}
            className="mt-3.5"
            disabled={opening}
            onClick={() => void openBillingWindow()}
          >
            {opening ? "결제창 여는 중…" : registered ? "카드 변경" : "카드 등록"}
          </Button>
        ) : null}

        {/* 누르면 아무 일도 안 나는 버튼을 두지 않는다. 왜 없는지 적는다. */}
        {owner && !configured ? (
          <Notice tone="warn" className="mt-3.5">
            결제가 아직 연결되지 않았습니다. 담당자에게 문의해 주세요.
          </Notice>
        ) : null}

        {!owner ? (
          <p className="mt-3 text-[12px] text-slate-2">결제수단은 소유자만 관리할 수 있습니다.</p>
        ) : null}

        {registered ? (
          <p className="mt-3 text-[11.5px] leading-relaxed text-slate-2">
            카드를 삭제하면 다음 청구가 실패합니다. 유료 이용 중이라면 새 카드를 먼저
            등록해 주세요.
          </p>
        ) : null}

        {error ? (
          <Notice tone="error" className="mt-3">
            {error}
          </Notice>
        ) : null}
      </CardBody>
    </Card>
  );
}
