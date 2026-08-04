"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";

/** 위젯이 동작해도 되는 주소. lastCalledAt 이 null 이면 아직 설치가 확인되지 않은 것이다. */
export interface AllowedOrigin {
  id: string;
  origin: string;
  lastCalledAt: string | null;
}

const allowedOriginSchema = z.object({
  id: z.string(),
  origin: z.string(),
  lastCalledAt: z.string().nullable(),
});

export const allowedOriginKeys = {
  list: ["allowed-origin", "list"] as const,
};

export function useAllowedOriginsQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<AllowedOrigin[]>({
    queryKey: allowedOriginKeys.list,
    enabled: accessToken !== null,
    queryFn: async () =>
      z.array(allowedOriginSchema).parse(await api("/api/app/allowed-origins")),
  });
}

function useInvalidateOrigins() {
  const queryClient = useQueryClient();
  return () => void queryClient.invalidateQueries({ queryKey: allowedOriginKeys.list });
}

export function useAddAllowedOrigin() {
  const invalidate = useInvalidateOrigins();
  return useMutation({
    mutationFn: (origin: string) =>
      api("/api/app/allowed-origins", { method: "POST", body: { origin } }),
    onSuccess: invalidate,
  });
}

export function useRemoveAllowedOrigin() {
  const invalidate = useInvalidateOrigins();
  return useMutation({
    mutationFn: (id: string) => api(`/api/app/allowed-origins/${id}`, { method: "DELETE" }),
    onSuccess: invalidate,
  });
}
