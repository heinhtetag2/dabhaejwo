import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api, type PageResponse } from "@/shared/api/http-client";

import type { Ticket, TicketStatus } from "./types";

export const ticketKeys = {
  all: ["ticket"] as const,
  list: (status: TicketStatus | undefined) => [...ticketKeys.all, "list", status ?? "ALL"] as const,
};

/** 정렬은 서버가 경과 시간 내림차순으로 고정한다 — 오래된 것이 위로 온다. */
export function useTicketListQuery(status?: TicketStatus) {
  return useQuery({
    queryKey: ticketKeys.list(status),
    queryFn: () => api<PageResponse<Ticket>>("/api/ops/tickets", { query: { status } }),
  });
}

export function useUpdateTicketStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ ticketId, status }: { ticketId: number; status: TicketStatus }) =>
      api<Ticket>(`/api/ops/tickets/${ticketId}`, { method: "PATCH", body: { status } }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ticketKeys.all }),
  });
}
