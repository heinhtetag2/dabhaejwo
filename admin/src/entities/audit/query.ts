import { useQuery } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";

import type { AuditListParams, AuditLog } from "./types";

export const auditKeys = {
  all: ["audit-log"] as const,
  list: (params: AuditListParams) => [...auditKeys.all, "list", params] as const,
};

/** 기간을 안 주면 서버가 최근 30일로 잡는다 — 3년치 전 구간 조회를 기본으로 두지 않는다. */
export function useAuditLogListQuery(params: AuditListParams) {
  return useQuery({
    queryKey: auditKeys.list(params),
    queryFn: () =>
      api<PageResponse<AuditLog>>("/api/ops/audit-logs", {
        query: {
          tenantId: params.tenantId,
          operatorId: params.operatorId,
          action: params.action,
          from: params.from,
          to: params.to,
          page: params.page ?? 0,
        },
      }),
  });
}
