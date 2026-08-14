"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/** 요금제 화면. 키는 api-contracts.md §9-3 과 동일하다. */
export type BillingStatus = "PAID" | "FAILED" | "PENDING" | "REFUNDED";

export interface BillingItem {
  id: number;
  period: string;
  amount: number;
  status: BillingStatus;
  failureReason: string | null;
  /** PG 미연동이라 항상 null 이다. */
  receiptUrl: string | null;
}

export interface PlanOverview {
  plan: { id: string; name: string; monthlyFee: number };
  usage: { convCount: number; convLimit: number; docCount: number; docLimit: number };
  nextBillingDate: string | null;
  /** 대화가 없으면 null. 0% 로 내리면 "공통 질문이 안 쓰였다"로 오해된다. */
  savedAnswerPercent: number | null;
  /**
   * 서비스별 이번 달 사용량.
   *
   * 한도는 업체 합산이라 "왜 한도가 찼는지"를 업체가 스스로 볼 방법이 없었다.
   * 합이 `usage.convCount` 보다 작을 수 있다 — 서비스 구분 이전 호출은 귀속을 복원할 수 없다.
   */
  botUsage: Array<{ botId: string; botName: string; convCount: number; docCount: number }>;
  billingRecords: BillingItem[];
}

export const BILLING_STATUS_LABEL: Record<BillingStatus, string> = {
  PAID: "결제됨",
  FAILED: "실패",
  PENDING: "대기",
  REFUNDED: "환불됨",
};

const planOverviewSchema = z.object({
  plan: z.object({ id: z.string(), name: z.string(), monthlyFee: z.number() }),
  usage: z.object({
    convCount: z.number(),
    convLimit: z.number(),
    docCount: z.number(),
    docLimit: z.number(),
  }),
  nextBillingDate: z.string().nullable(),
  savedAnswerPercent: z.number().nullable(),
  botUsage: z.array(
    z.object({
      botId: z.string(),
      botName: z.string(),
      convCount: z.number(),
      docCount: z.number(),
    }),
  ),
  billingRecords: z.array(
    z.object({
      id: z.number(),
      period: z.string(),
      amount: z.number(),
      status: z.enum(["PAID", "FAILED", "PENDING", "REFUNDED"]),
      failureReason: z.string().nullable(),
      receiptUrl: z.string().nullable(),
    }),
  ),
});

export const planKeys = {
  overview: ["plan", "detail", "overview"] as const,
};

/**
 * 유료 전환 신청. PG 연동 전까지 문의로 접수된다 (tenant-public-plan.md §5.2).
 * 결제가 일어나지 않으므로 성공해도 요금제가 바뀌지 않는다 — 화면이 그 사실을 알린다.
 */
/**
 * 요금제 변경 — <b>고르면 즉시 결제된다.</b>
 *
 * 무료·체험에서 올라올 때만 지금 돈이 나간다. 유료끼리는 요금제만 바뀌고
 * 새 금액은 다음 청구일부터다 — 이번 달치를 두 번 받지 않기 위해서다.
 */
export function usePlanChange() {
  const queryClient = useQueryClient();
  return useMutation<PlanChangeResult, unknown, string>({
    mutationFn: (planCode: string) =>
      api<PlanChangeResult>("/api/app/plan/change", { method: "POST", body: { planCode } }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: planKeys.overview });
      void queryClient.invalidateQueries({ queryKey: ["billing"] });
      // 요금제가 바뀌면 사이드바의 한도 표시도 달라진다.
      void queryClient.invalidateQueries({ queryKey: ["session"] });
    },
  });
}

export interface PlanChangeResult {
  planName: string;
  /** true 면 이번에 실제로 돈이 나갔다. 화면이 문구를 갈라 써야 한다. */
  charged: boolean;
  amountKrw: number;
  receiptUrl: string | null;
  nextBillingDate: string | null;
}

export function useUpgradeRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { planCode: string; note?: string }) =>
      api("/api/app/plan/upgrade-request", { method: "POST", body: input }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: planKeys.overview }),
  });
}

export function usePlanOverviewQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<PlanOverview>({
    queryKey: planKeys.overview,
    enabled: accessToken !== null,
    queryFn: async () => planOverviewSchema.parse(await api("/api/app/plan")),
  });
}
