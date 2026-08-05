/** 오늘 화면. 키는 api-contracts.md §5 와 일치한다. */

export type ActionType =
  | "COST_EXCEEDED"
  | "JOB_FAILED"
  | "PAYMENT_FAILED"
  | "TRIAL_ENDING"
  | "TICKET_WAITING";

export interface TodayAction {
  type: ActionType;
  tenantId: string | null;
  title: string;
  detail: string;
  /** 누르면 바로 그 대상으로 간다 — 이름만 확인하고 다시 검색하게 만들지 않는다. */
  targetPath: string;
}

/**
 * 시스템 상태.
 *
 * **측정 지점이 없는 값은 `null` 이다.** 답변 파이프라인·크롤러 워커·APM 이 아직 없어
 * 응답 시간·워커 가동·벡터 DB 사용량·5xx 건수는 잴 곳이 없다. 화면은 "집계 없음"으로
 * 표시한다 — 0 으로 보이면 정상이라고 읽힌다.
 */
export interface TodaySystem {
  chatApiP95Ms: number | null;
  embedQueueDepth: number;
  crawlerWorkers: string | null;
  vectorDbUsagePercent: number | null;
  todayError5xxCount: number | null;
  recentErrors: { at: string; tenantName: string | null; code: string | null }[];
}

export interface TodaySummary {
  headline: { tenantCount: number; costExceededCount: number };
  stats: {
    payingTenantCount: number;
    mrrKrw: number;
    todayConvCount: number;
    todayCostKrw: number;
  };
  actions: TodayAction[];
  system: TodaySystem;
  /** 당일 집계가 몇 시 기준인지. 한 번도 집계된 적 없으면 null. */
  aggregatedAt: string | null;
}
