import { useMutation, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import { tenantKeys } from "./query";
import type {
  ImpersonationSession,
  QuotaOverride,
  TenantDetail,
  TenantNote,
  TenantStatus,
} from "./types";

/**
 * 업체 조치. 성공하면 상세·목록·활동 이력을 함께 invalidate 한다 —
 * 조치는 감사 기록에 남으므로 활동 이력도 즉시 달라진다.
 */
function useTenantMutation<TVariables, TData>(
  tenantId: string,
  fn: (variables: TVariables) => Promise<TData>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: tenantKeys.all });
      void queryClient.invalidateQueries({ queryKey: tenantKeys.activities(tenantId) });
    },
  });
}

export function useChangeTenantStatus(tenantId: string) {
  return useTenantMutation<{ status: TenantStatus; reason: string }, TenantDetail>(
    tenantId,
    (body) =>
      api<TenantDetail>(`/api/ops/tenants/${tenantId}/status`, { method: "PATCH", body }),
  );
}

export function useChangeTenantPlan(tenantId: string) {
  return useTenantMutation<{ planId: string; reason: string }, TenantDetail>(tenantId, (body) =>
    api<TenantDetail>(`/api/ops/tenants/${tenantId}/plan`, { method: "PATCH", body }),
  );
}

export function useExtendTrial(tenantId: string) {
  return useTenantMutation<{ days: number; reason: string }, TenantDetail>(tenantId, (body) =>
    api<TenantDetail>(`/api/ops/tenants/${tenantId}/trial-extension`, { method: "POST", body }),
  );
}

export function useGrantQuota(tenantId: string) {
  return useTenantMutation<
    { convDelta: number; docDelta: number; reason: string },
    QuotaOverride
  >(tenantId, (body) =>
    api<QuotaOverride>(`/api/ops/tenants/${tenantId}/quota-overrides`, { method: "POST", body }),
  );
}

export function useAddTenantNote(tenantId: string) {
  return useTenantMutation<{ body: string }, TenantNote>(tenantId, (body) =>
    api<TenantNote>(`/api/ops/tenants/${tenantId}/notes`, { method: "POST", body }),
  );
}

/**
 * 대리 로그인. 사유 없이는 서버가 거부한다 — 화면의 필수 표시는 UX 일 뿐이다.
 *
 * 성공하면 업체 대시보드로 넘길 토큰을 받는다. 그 토큰은 VIEWER 권한이라
 * 운영자가 업체 설정을 바꿀 수 없다.
 */
export function useStartImpersonation(tenantId: string) {
  return useTenantMutation<{ reason: string }, ImpersonationSession>(tenantId, (body) =>
    api<ImpersonationSession>(`/api/ops/tenants/${tenantId}/impersonate`, {
      method: "POST",
      body,
    }),
  );
}
