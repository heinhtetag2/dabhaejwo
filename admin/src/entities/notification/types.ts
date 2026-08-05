/**
 * 알림. api-contracts.md §11.
 *
 * 종류 문자열은 api 의 `NotificationType` enum 과 같은 커밋에서 움직인다.
 * 여기 없는 값이 내려와도 목록은 깨지지 않는다 — 아이콘·라벨만 기본값으로 떨어진다.
 */
export type OpsNotificationType =
  | "TENANT_SIGNED_UP"
  | "TICKET_OPENED"
  | "GLOBAL_COST_CAP_WARNING"
  | "TENANT_COST_CAP_REACHED"
  | "TENANT_COST_EXCEEDED"
  | "TRIAL_ENDING_SOON"
  | "INDEXING_FAILURES"
  | "PAYMENT_RECEIVED";

export type NotificationSeverity = "LOW" | "NORMAL" | "HIGH";

export interface AppNotification {
  id: number;
  type: OpsNotificationType;
  severity: NotificationSeverity;
  title: string;
  body: string | null;
  /** 눌렀을 때 갈 곳. 없으면 이동하지 않고 읽음만 처리한다. */
  targetPath: string | null;
  read: boolean;
  createdAt: string;
}
