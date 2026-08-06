"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { useRegisterBillingMethod } from "@/entities/tenant/billing";
import { ApiError } from "@/shared/api/http-client";
import { ROUTES } from "@/shared/config/routes";
import { Notice } from "@/shared/ui/notice";

/**
 * 결제창에서 돌아왔을 때.
 *
 * <p>토스가 성공 URL 에 {@code customerKey} 와 {@code authKey} 를 붙여 보낸다.
 * 그 인증키를 서버로 넘겨야 <b>빌링키</b>가 발급된다 — 여기서 멈추면 카드는 등록되지 않는다.
 *
 * <p>딱 한 번만 보낸다. 인증키는 일회용이라 두 번째 요청은 반드시 실패하는데,
 * 그 실패가 "카드 등록에 실패했습니다"로 보이면 사용자는 멀쩡한데도 다시 시도한다.
 */
export function BillingReturn() {
  const params = useSearchParams();
  const router = useRouter();
  const register = useRegisterBillingMethod();
  const sent = useRef(false);

  const result = params.get("billing");
  const authKey = params.get("authKey");
  const customerKey = params.get("customerKey");

  /*
   * 실패 문구는 <b>첫 렌더에서 한 번</b> 읽는다. 아래 effect 가 주소창을 비우기 때문에
   * 매 렌더 params 를 다시 읽으면 문구가 곧바로 사라진다.
   */
  const [failMessage] = useState(() =>
    result === "fail" ? (params.get("message") ?? "카드 등록이 취소되었습니다.") : null,
  );

  useEffect(() => {
    if (result === "fail") {
      // 사용자가 결제창을 닫은 경우도 여기로 온다. 주소만 정리한다.
      router.replace(ROUTES.plan);
      return;
    }
    if (result !== "success" || !authKey || !customerKey || sent.current) {
      return;
    }
    sent.current = true;
    register.mutate({ authKey, customerKey });
    // 주소창에서 인증키를 지운다 — 새로고침으로 다시 보내지 않게, 그리고 남지 않게.
    router.replace(ROUTES.plan);
  }, [result, authKey, customerKey, register, router]);

  if (failMessage) {
    return (
      <Notice tone="error" className="mb-4">
        {failMessage}
      </Notice>
    );
  }
  if (register.isPending) {
    return (
      <Notice tone="info" className="mb-4">
        카드를 등록하는 중입니다…
      </Notice>
    );
  }
  if (register.isSuccess) {
    return (
      <Notice tone="info" className="mb-4">
        카드를 등록했습니다. 다음 청구부터 자동으로 결제됩니다.
      </Notice>
    );
  }
  if (register.isError) {
    return (
      <Notice tone="error" className="mb-4">
        {register.error instanceof ApiError
          ? register.error.message
          : "카드를 등록하지 못했습니다. 잠시 후 다시 시도해 주세요."}
      </Notice>
    );
  }
  return null;
}
