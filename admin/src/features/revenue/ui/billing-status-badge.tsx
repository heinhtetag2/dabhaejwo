import type { BillingStatus } from "@/entities/revenue";
import { Badge, type BadgeTone } from "@/shared/common/badge";

const LABEL: Record<BillingStatus, { text: string; tone: BadgeTone }> = {
  PAID: { text: "결제 완료", tone: "ok" },
  FAILED: { text: "결제 실패", tone: "error" },
  PENDING: { text: "대기", tone: "warn" },
  REFUNDED: { text: "환불", tone: "idle" },
};

export function BillingStatusBadge({ status }: { status: BillingStatus }) {
  const { text, tone } = LABEL[status];
  return <Badge tone={tone}>{text}</Badge>;
}
