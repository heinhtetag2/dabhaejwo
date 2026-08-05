import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";
import type { ProviderName } from "@/entities/usage";

import type { Plan, PlanModelAssignment } from "./types";

export const planKeys = {
  all: ["plan"] as const,
  list: () => [...planKeys.all, "list"] as const,
  assignments: () => [...planKeys.all, "assignments"] as const,
};

export function usePlanListQuery() {
  return useQuery({
    queryKey: planKeys.list(),
    queryFn: () => api<Plan[]>("/api/ops/plans"),
  });
}

export function usePlanAssignmentsQuery() {
  return useQuery({
    queryKey: planKeys.assignments(),
    queryFn: () => api<PlanModelAssignment[]>("/api/ops/plan-model-assignments"),
  });
}

export interface PlanUpdateBody {
  name: string;
  monthlyFee: number;
  negotiable: boolean;
  convLimit: number;
  docLimit: number;
  sellable: boolean;
}

export function useUpdatePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, body }: { planId: string; body: PlanUpdateBody }) =>
      api<Plan>(`/api/ops/plans/${planId}`, { method: "PATCH", body }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: planKeys.all }),
  });
}

export function useCreatePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: PlanUpdateBody & { code: string; sortOrder: number }) =>
      api<Plan>("/api/ops/plans", { method: "POST", body }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: planKeys.all }),
  });
}

export function useSaveAssignment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: {
      planId: string;
      provider: ProviderName;
      model: string;
      chunkCount: number;
    }) => api<PlanModelAssignment>("/api/ops/plan-model-assignments", { method: "PUT", body }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: planKeys.assignments() }),
  });
}
