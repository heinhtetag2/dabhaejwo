/**
 * 알림. api-contracts.md §11.
 *
 * 종류 문자열은 api 의 `NotificationType` enum 과 같은 커밋에서 움직인다.
 * 업체가 받는 종류만 여기 있다 — 운영자용 종류는 admin 쪽에 따로 있다.
 */
export type TenantNotificationType =
  | "IMPERSONATION_STARTED"
  | "QUOTA_WARNING"
  | "QUOTA_EXHAUSTED"
  | "TRIAL_ENDING"
  | "INDEXING_DONE"
  | "INDEXING_FAILED"
  | "LEAD_RECEIVED"
  | "ANSWER_GAPS_PILING";

export type NotificationSeverity = "LOW" | "NORMAL" | "HIGH";

export interface AppNotification {
  id: number;
  type: TenantNotificationType;
  severity: NotificationSeverity;
  title: string;
  body: string | null;
  /** 눌렀을 때 갈 곳. 없으면 이동하지 않고 읽음만 처리한다. */
  targetPath: string | null;
  read: boolean;
  createdAt: string;
}
