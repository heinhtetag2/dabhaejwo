import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/shared/api/http-client";

import type {
  Operator,
  OperatorCreateBody,
  OperatorUpdateBody,
  RolePermissions,
} from "./types";

export const operatorKeys = {
  all: ["operator"] as const,
  list: () => [...operatorKeys.all, "list"] as const,
  rolePermissions: () => [...operatorKeys.all, "role-permissions"] as const,
};

export function useOperatorListQuery() {
  return useQuery({
    queryKey: operatorKeys.list(),
    queryFn: () => api<Operator[]>("/api/ops/operators"),
  });
}

export function useRolePermissionsQuery() {
  return useQuery({
    queryKey: operatorKeys.rolePermissions(),
    queryFn: () => api<RolePermissions[]>("/api/ops/operators/role-permissions"),
    // 역할→권한 매핑은 배포로만 바뀐다. 화면을 볼 때마다 다시 받을 이유가 없다.
    staleTime: 10 * 60_000,
  });
}

function useOperatorMutation<TVariables>(fn: (variables: TVariables) => Promise<Operator>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: operatorKeys.all }),
  });
}

export function useCreateOperator() {
  return useOperatorMutation((body: OperatorCreateBody) =>
    api<Operator>("/api/ops/operators", { method: "POST", body }),
  );
}

export function useUpdateOperator() {
  return useOperatorMutation(({ id, body }: { id: string; body: OperatorUpdateBody }) =>
    api<Operator>(`/api/ops/operators/${id}`, { method: "PATCH", body }),
  );
}

export function useResetOperatorPassword() {
  return useOperatorMutation(
    ({ id, password, reason }: { id: string; password: string; reason: string }) =>
      api<Operator>(`/api/ops/operators/${id}/password`, {
        method: "PATCH",
        body: { password, reason },
      }),
  );
}

/**
 * 비활성화·복구. **삭제가 아니다** — 운영자 삭제 엔드포인트는 존재하지 않는다.
 * 감사 기록이 행위자를 FK 로 참조하고 3년 보존이라 지울 수 없다.
 */
export function useChangeOperatorActive() {
  return useOperatorMutation(
    ({ id, active, reason }: { id: string; active: boolean; reason: string }) =>
      api<Operator>(`/api/ops/operators/${id}/active`, {
        method: "PATCH",
        body: { active, reason },
      }),
  );
}
