/** 수익성 · AI 사용량. 키는 api-contracts.md §6 과 일치한다. */

import type { PageResponse } from "@/shared/api/http-client";

export type UsagePurpose = "ANSWER" | "EMBED_DOC" | "EMBED_QUERY" | "ETC";
export type ProviderName = "GOOGLE" | "ANTHROPIC" | "OPENAI" | "STUB";

export interface ProfitabilityItem {
  tenant: { id: string; name: string };
  planName: string | null;
  billedKrw: number;
  costKrw: number;
  costRatioPercent: number;
  savedAnswerPercent: number;
}

export interface ProfitabilityStats {
  revenueKrw: number;
  costKrw: number;
  avgCostRatioPercent: number;
  savedAnswerPercent: number;
  costExceededCount: number;
  /** 경고선은 cost_guards 설정값이다. 프론트가 70을 상수로 갖고 있으면 설정을 바꿔도 색이 안 바뀐다. */
  costRatioWarnPercent: number;
}

export type Profitability = { stats: ProfitabilityStats } & PageResponse<ProfitabilityItem>;

export interface AiUsageSummary {
  todayTokensIn: number;
  todayTokensOut: number;
  todayCostKrw: number;
  /** 오늘 대화가 없으면 null — 0원으로 보이면 "공짜로 돌고 있다"로 읽힌다. */
  costPerConvKrw: number | null;
  monthCostKrw: number;
  /** 경과일 평균 × 그 달의 일수. 추정치다. */
  monthProjectedCostKrw: number;
}

/** 누적 막대의 한 칸. 층 순서가 화면 계약이라 고정 필드다. */
export interface DailyCost {
  day: string;
  answerKrw: number;
  embedDocKrw: number;
  embedQueryKrw: number;
  etcKrw: number;
}

export interface ModelUsage {
  provider: ProviderName;
  model: string;
  purpose: UsagePurpose;
  callCount: number;
  inputTokens: number;
  outputTokens: number;
  costKrw: number;
  sharePercent: number;
}

export interface TopTenantUsage {
  tenant: { id: string; name: string };
  planName: string | null;
  tokens: number;
  costKrw: number;
  costPerConvKrw: number | null;
}
