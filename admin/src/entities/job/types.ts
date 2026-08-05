/** 작업 큐. 키는 api-contracts.md §8 과 일치한다. */

export type JobKind = "CRAWL" | "RECRAWL" | "EMBED_DOC";
export type JobStatus = "QUEUED" | "RUNNING" | "DONE" | "FAILED";

export interface Job {
  id: number;
  kind: JobKind;
  tenant: { id: string; name: string } | null;
  target: string;
  status: JobStatus;
  /** 운영자용이라 원문 그대로 온다. 한글 설명은 화면이 매핑한다. */
  errorCode: string | null;
  attempts: number;
  maxAttempts: number;
  retriable: boolean;
  updatedAt: string;
}

export interface JobStats {
  queuedCount: number;
  runningCount: number;
  doneTodayCount: number;
  /** 오늘 처리된 작업이 없으면 null — 0% 로 내려오면 "전부 실패"로 읽힌다. */
  successPercent: number | null;
  failedCount: number;
}
