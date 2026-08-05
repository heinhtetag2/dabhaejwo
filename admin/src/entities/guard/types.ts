/** 비용 안전장치. 키는 api-contracts.md §7 과 일치한다. */

export type QuotaExceededBehavior = "STOP_AND_NOTICE" | "OVERAGE_BILLING" | "NOTIFY_ONLY";

export interface CostGuard {
  tenantDailyCapKrw: number;
  globalDailyCapKrw: number;
  ipQuestionsPerMin: number;
  bulkUploadLimit: number;
  costRatioWarnPercent: number;
  answerFailSimilarity: number;
  defaultChunkCount: number;
  answerMaxLength: number;
  churnPurgeGraceDays: number;
  quotaExceededBehavior: QuotaExceededBehavior;
  slackAlertEnabled: boolean;
  commonPrompt: string;
  /** 문서·질문을 임베딩할 공급사. 바꾸면 기존 조각이 무효가 되어 다시 학습해야 한다. */
  embeddingProvider: string;
  updatedAt: string;
}

/**
 * PUT 이라 전체 필드를 보낸다 — 부분 수정은 안 보이던 값이 조용히 덮이게 만든다.
 *
 * 단 `embeddingProvider` 는 빠진다. 별도 경로를 쓰는 이유는 결과가 전혀 다르기 때문이다 —
 * 슬랙 토글과 같은 저장 버튼에 묶이면 클릭 한 번에 전 조각이 무효가 된다.
 */
export type CostGuardUpdateBody = Omit<CostGuard, "updatedAt" | "embeddingProvider"> & {
  reason: string;
};
