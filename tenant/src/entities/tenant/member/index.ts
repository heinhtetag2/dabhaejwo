"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";

import { api } from "@/shared/api/http-client";
import { useAuthStore } from "@/shared/lib/auth-store";
import type { Member, TenantMemberRole } from "@/entities/auth/session";

/** 팀원 목록·초대. Member 타입은 세션 엔티티에서 가져온다 — 같은 리소스를 두 번 정의하지 않는다. */
export type { Member, TenantMemberRole } from "@/entities/auth/session";

export const MEMBER_ROLE_LABEL: Record<TenantMemberRole, string> = {
  OWNER: "소유자",
  EDITOR: "편집",
  VIEWER: "보기만",
};

const memberSchema = z.object({
  id: z.string(),
  name: z.string().nullable(),
  email: z.string(),
  role: z.enum(["OWNER", "EDITOR", "VIEWER"]),
  inviteState: z.enum(["PENDING", "ACCEPTED"]),
  lastSeenAt: z.string().nullable(),
});

export const memberKeys = {
  list: ["member", "list"] as const,
};

export function useMembersQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<Member[]>({
    queryKey: memberKeys.list,
    enabled: accessToken !== null,
    queryFn: async () => z.array(memberSchema).parse(await api("/api/app/members")),
  });
}

function useInvalidateMembers() {
  const queryClient = useQueryClient();
  return () => void queryClient.invalidateQueries({ queryKey: memberKeys.list });
}

export function useInviteMember() {
  const invalidate = useInvalidateMembers();
  return useMutation({
    mutationFn: (input: { email: string; name?: string; role: TenantMemberRole }) =>
      api("/api/app/members", { method: "POST", body: input }),
    onSuccess: invalidate,
  });
}

export function useChangeMemberRole() {
  const invalidate = useInvalidateMembers();
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: TenantMemberRole }) =>
      api(`/api/app/members/${id}`, { method: "PATCH", body: { role } }),
    onSuccess: invalidate,
  });
}

export function useRemoveMember() {
  const invalidate = useInvalidateMembers();
  return useMutation({
    mutationFn: (id: string) => api(`/api/app/members/${id}`, { method: "DELETE" }),
    onSuccess: invalidate,
  });
}

/** 운영팀 접속 이력. 업체에게 공개된다 (tenant-plan.md §6.3). */
export interface ImpersonationHistoryItem {
  sessionId: string;
  reason: string;
  startedAt: string;
  endedAt: string | null;
  status: "ACTIVE" | "ENDED" | "EXPIRED" | "REVOKED";
}

const historySchema = z.object({
  sessionId: z.string(),
  reason: z.string(),
  startedAt: z.string(),
  endedAt: z.string().nullable(),
  status: z.enum(["ACTIVE", "ENDED", "EXPIRED", "REVOKED"]),
});

export function useImpersonationHistoryQuery() {
  const accessToken = useAuthStore((state) => state.accessToken);

  return useQuery<ImpersonationHistoryItem[]>({
    queryKey: ["impersonation", "list", "history"],
    enabled: accessToken !== null,
    queryFn: async () =>
      z.array(historySchema).parse(await api("/api/app/impersonation/history")),
  });
}
