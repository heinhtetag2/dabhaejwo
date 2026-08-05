import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import type { CostGuard, CostGuardUpdateBody } from "./types";

export const guardKeys = {
  all: ["cost-guard"] as const,
  current: () => [...guardKeys.all, "current"] as const,
};

export function useCostGuardQuery() {
  return useQuery({
    queryKey: guardKeys.current(),
    queryFn: () => api<CostGuard>("/api/ops/cost-guards"),
  });
}

export function useUpdateCostGuard() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CostGuardUpdateBody) =>
      api<CostGuard>("/api/ops/cost-guards", { method: "PUT", body }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: guardKeys.all }),
  });
}
