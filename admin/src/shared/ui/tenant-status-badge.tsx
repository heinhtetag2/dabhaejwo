import { Badge, type BadgeTone } from "@/shared/common/badge";
import type { TenantStatus } from "@/entities/tenant/types";

const LABEL: Record<TenantStatus, { text: string; tone: BadgeTone }> = {
  TRIAL: { text: "체험 중", tone: "info" },
  ACTIVE: { text: "정상", tone: "ok" },
  SUSPENDED: { text: "일시정지", tone: "warn" },
  CHURNED: { text: "해지", tone: "idle" },
};

export function TenantStatusBadge({ status }: { status: TenantStatus }) {
  const { text, tone } = LABEL[status];
  return <Badge tone={tone}>{text}</Badge>;
}
