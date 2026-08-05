import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import type { ModelPrice, ModelPriceCreateBody } from "./types";

export const pricingKeys = {
  all: ["model-price"] as const,
  list: () => [...pricingKeys.all, "list"] as const,
};

export function useModelPriceListQuery() {
  return useQuery({
    queryKey: pricingKeys.list(),
    queryFn: () => api<ModelPrice[]>("/api/ops/model-prices"),
  });
}

/**
 * 새 단가 등록. **기존 행을 고치는 것이 아니다.**
 *
 * 화면도 인라인 수정이 아니라 "새 단가 + 적용 시점"이어야 한다 — 프로토타입은
 * 표 안 input 으로 직접 고치는 형태인데, 그대로 옮기면 소급 변경이 된다.
 */
export function useRegisterModelPrice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: ModelPriceCreateBody) =>
      api<ModelPrice>("/api/ops/model-prices", { method: "POST", body }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: pricingKeys.all }),
  });
}
