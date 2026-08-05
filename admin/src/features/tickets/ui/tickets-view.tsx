"use client";

import { useTicketListQuery, useUpdateTicketStatus, type TicketStatus } from "@/entities/ticket";
import { Badge, type BadgeTone } from "@/shared/common/badge";
import { Button } from "@/shared/common/button";
import { Card, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th } from "@/shared/common/table";
import { errorMessage } from "@/shared/lib/error-message";
import { PageHeader } from "@/widgets/page-header/page-header";

const STATUS: Record<TicketStatus, { text: string; tone: BadgeTone }> = {
  OPEN: { text: "대기", tone: "error" },
  ANSWERED: { text: "답변함", tone: "warn" },
  CLOSED: { text: "종료", tone: "idle" },
};

/** 경과 시간. 오래된 것이 위로 오므로 첫 줄이 가장 급한 건이다. */
function elapsed(minutes: number): string {
  if (minutes < 60) return `${minutes}분`;
  const hours = Math.floor(minutes / 60);
  if (hours < 48) return `${hours}시간`;
  return `${Math.floor(hours / 24)}일`;
}

export function TicketsView() {
  const { data, isPending, isError, error, refetch } = useTicketListQuery();
  const update = useUpdateTicketStatus();

  const openCount = (data?.content ?? []).filter((t) => t.status === "OPEN").length;

  return (
    <>
      <PageHeader title="문의" description="오래된 것이 위로 옵니다" />

      <Card>
        <CardHeader title="문의" aside={<Eyebrow>답변 대기 {openCount}건</Eyebrow>} />

        {isPending ? <LoadingState /> : null}
        {isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : null}
        {data && data.content.length === 0 ? <EmptyState message="접수된 문의가 없습니다" /> : null}

        {data && data.content.length > 0 ? (
          <Table>
            <thead>
              <tr>
                <Th className="w-[130px]">업체</Th>
                <Th>내용</Th>
                <Th className="w-[92px]">경과</Th>
                <Th className="w-[82px]">상태</Th>
                <Th className="w-[150px]" />
              </tr>
            </thead>
            <tbody>
              {data.content.map((ticket) => (
                <tr key={ticket.id}>
                  <Td className="font-medium">{ticket.tenant.name}</Td>
                  <Td>
                    <div>{ticket.subject}</div>
                    <div className="text-[11.5px] text-slate-2">{ticket.body}</div>
                  </Td>
                  <Td className="tabular text-[12.5px] text-slate-2">
                    {elapsed(ticket.elapsedMinutes)}
                  </Td>
                  <Td>
                    <Badge tone={STATUS[ticket.status].tone}>{STATUS[ticket.status].text}</Badge>
                  </Td>
                  <Td>
                    {/* 답변 본문은 여기 남기지 않는다 — 회신은 이메일로 나가고
                        사본을 두면 두 곳이 갈라진다 */}
                    <div className="flex gap-1.5">
                      {ticket.status === "OPEN" ? (
                        <Button
                          size="sm"
                          disabled={update.isPending}
                          onClick={() =>
                            update.mutate({ ticketId: ticket.id, status: "ANSWERED" })
                          }
                        >
                          답변함
                        </Button>
                      ) : null}
                      {ticket.status !== "CLOSED" ? (
                        <Button
                          size="sm"
                          disabled={update.isPending}
                          onClick={() => update.mutate({ ticketId: ticket.id, status: "CLOSED" })}
                        >
                          종료
                        </Button>
                      ) : null}
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : null}
      </Card>
    </>
  );
}
