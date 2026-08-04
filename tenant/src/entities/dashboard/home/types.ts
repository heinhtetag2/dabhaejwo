/** 홈 요약. 키는 api-contracts.md §9-4 와 동일하다. */

export interface KnowledgeStatus {
  documentCount: number;
  indexedCount: number;
  processingCount: number;
  failedCount: number;
}

export interface TopQuestion {
  question: string;
  askCount: number;
}

export interface HomeSummary {
  todayConvCount: number;
  /** 어제 대비 증감. 음수면 줄어든 것이다. */
  todayConvDelta: number;
  /** 대화가 한 건도 없으면 null. 0 으로 내리면 "다 실패했다"로 읽힌다. */
  answerSuccessPercent: number | null;
  answerSuccessPercentLastWeek: number | null;
  openGapCount: number;
  todayLeadCount: number;
  weekLeadCount: number;
  /** 잰 적이 없으면 null — 답변 파이프라인이 붙기 전까지는 항상 null 이다. */
  avgResponseMs: number | null;
  knowledge: KnowledgeStatus;
  topQuestions: TopQuestion[];
}
