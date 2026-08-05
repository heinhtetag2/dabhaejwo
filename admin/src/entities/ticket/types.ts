/** 문의 티켓. 키는 api-contracts.md §8 과 일치한다. */

import type { OperatorRef } from "@/entities/tenant";

export type TicketStatus = "OPEN" | "ANSWERED" | "CLOSED";

export interface Ticket {
  id: number;
  tenant: { id: string; name: string };
  subject: string;
  body: string;
  status: TicketStatus;
  /** 서버가 계산해 준다 — 정렬 기준이 경과 시간이라 화면이 시계를 따로 굴리면 어긋난다. */
  elapsedMinutes: number;
  answeredBy: OperatorRef | null;
  answeredAt: string | null;
  createdAt: string;
}
