import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import type { FeatureFlag, FeatureFlagUpdateBody } from "./types";

export const flagKeys = {
  all: ["feature-flag"] as const,
  list: () => [...flagKeys.all, "list"] as const,
};

export function useFeatureFlagListQuery() {
  return useQuery({
    queryKey: flagKeys.list(),
    queryFn: () => api<FeatureFlag[]>("/api/ops/feature-flags"),
  });
}

export function useUpdateFeatureFlag() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, body }: { key: string; body: FeatureFlagUpdateBody }) =>
      api<FeatureFlag>(`/api/ops/feature-flags/${key}`, { method: "PATCH", body }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: flagKeys.all }),
  });
}
