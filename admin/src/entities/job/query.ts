import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";

import type { Job, JobStats, JobStatus } from "./types";

export const jobKeys = {
  all: ["job"] as const,
  list: (status: JobStatus | undefined) => [...jobKeys.all, "list", status ?? "ALL"] as const,
  stats: () => [...jobKeys.all, "stats"] as const,
};

export function useJobListQuery(status?: JobStatus) {
  return useQuery({
    queryKey: jobKeys.list(status),
    queryFn: () => api<PageResponse<Job>>("/api/ops/jobs", { query: { status } }),
  });
}

export function useJobStatsQuery() {
  return useQuery({
    queryKey: jobKeys.stats(),
    queryFn: () => api<JobStats>("/api/ops/jobs/stats"),
  });
}

/**
 * 재시도. **지금은 서버가 항상 FEATURE_NOT_READY(503)로 거절한다** — 큐에 다시 넣어도
 * 집어갈 워커가 없기 때문이다. 화면은 그 오류를 그대로 보여준다.
 * 성공한 척 넘기면 운영자는 복구된 줄 알고 기다린다.
 */
export function useRetryJob() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (jobId: number | "all") =>
      api<void>(jobId === "all" ? "/api/ops/jobs/retry-all" : `/api/ops/jobs/${jobId}/retry`, {
        method: "POST",
      }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: jobKeys.all }),
  });
}
