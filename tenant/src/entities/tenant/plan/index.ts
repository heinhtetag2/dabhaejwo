"use client";

import { useQuery } from "@tanstack/react-query";
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

export function usePlanOverviewQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<PlanOverview>({
    queryKey: planKeys.overview,
    enabled: accessToken !== null,
    queryFn: async () => planOverviewSchema.parse(await api("/api/app/plan")),
  });
}
