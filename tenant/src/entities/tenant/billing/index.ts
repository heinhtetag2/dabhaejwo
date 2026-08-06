"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/**
 * 등록된 결제수단.
 *
 * <b>빌링키는 여기 오지 않는다.</b> 화면이 알아야 할 것은 "어느 카드인지"뿐이다.
 */
export interface BillingMethod {
  registered: boolean;
  cardCompany: string | null;
  cardNumberMasked: string | null;
  cardType: string | null;
  registeredAt: string | null;
}

const billingMethodSchema = z.object({
  registered: z.boolean(),
  cardCompany: z.string().nullable(),
  cardNumberMasked: z.string().nullable(),
  cardType: z.string().nullable(),
  registeredAt: z.string().nullable(),
});

export const billingKeys = {
  method: ["billing", "detail", "method"] as const,
};

export function useBillingMethodQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<BillingMethod>({
    queryKey: billingKeys.method,
    enabled: accessToken !== null,
    queryFn: async () => billingMethodSchema.parse(await api("/api/app/billing/method")),
  });
}

function useInvalidateBilling() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: billingKeys.method });
    // 요금제 화면의 결제 내역·다음 결제일도 함께 바뀐다.
    void queryClient.invalidateQueries({ queryKey: ["plan"] });
  };
}

/** 결제창이 돌려준 인증키를 서버가 빌링키로 바꿔 저장한다. */
export function useRegisterBillingMethod() {
  const invalidate = useInvalidateBilling();
  return useMutation({
    mutationFn: (input: { authKey: string; customerKey: string }) =>
      api<BillingMethod>("/api/app/billing/method", { method: "POST", body: input }),
    onSuccess: invalidate,
  });
}

export function useRemoveBillingMethod() {
  const invalidate = useInvalidateBilling();
  return useMutation({
    mutationFn: () => api("/api/app/billing/method", { method: "DELETE" }),
    onSuccess: invalidate,
  });
}
