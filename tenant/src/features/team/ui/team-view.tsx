"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  MEMBER_ROLE_LABEL,
  useChangeMemberRole,
  useImpersonationHistoryQuery,
  useInviteMember,
  useMembersQuery,
  useRemoveMember,
  type TenantMemberRole,
} from "@/entities/tenant/member";
import { ApiError } from "@/shared/api/http-client";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { Notice } from "@/shared/ui/notice";
import { StatusBadge } from "@/shared/ui/status-badge";

/**
 * 팀원 · 운영팀 접속 이력.
 *
 * <p>초대·권한 변경·삭제는 소유자만 할 수 있다 (tenant-plan.md §8).
 * 권한의 진실은 서버이며 여기서 버튼을 감추는 것은 UX 일 뿐이다.
 */
export function TeamView() {
  const { data: context } = useAppContextQuery();
  const members = useMembersQuery();
  const history = useImpersonationHistoryQuery();

  const invite = useInviteMember();
  const changeRole = useChangeMemberRole();
  const remove = useRemoveMember();

  const [email, setEmail] = useState("");
  const [role, setRole] = useState<TenantMemberRole>("EDITOR");
  const [notice, setNotice] = useState<{ tone: "info" | "error"; text: string } | null>(null);

  const isOwner = context?.member?.role === "OWNER";

  const run = (action: () => Promise<unknown>, successText: string) => {
    setNotice(null);
    void action()
      .then(() => setNotice({ tone: "info", text: successText }))
      .catch((cause: unknown) =>
        setNotice({
          tone: "error",
          text: cause instanceof ApiError ? cause.message : "요청을 처리하지 못했습니다",
        }),
      );
  };

  return (
    <>
      <Card className="mb-4">
        <CardHeader
          title="팀원"
          aside={
            isOwner ? (
              <>
                <label className="sr-only" htmlFor="invite-email">
                  초대할 이메일
                </label>
                <input
                  id="invite-email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="name@example.com"
                  className="w-[200px] rounded-[7px] border border-line px-2.5 py-[5.5px] text-[12.5px] focus:border-ink-3 focus:outline-none"
                />
                <label className="sr-only" htmlFor="invite-role">
                  권한
                </label>
                <select
                  id="invite-role"
                  value={role}
                  onChange={(event) => setRole(event.target.value as TenantMemberRole)}
                  className="rounded-[7px] border border-line px-2 py-[5.5px] text-[12.5px]"
                >
                  <option value="EDITOR">편집</option>
                  <option value="VIEWER">보기만</option>
                </select>
                <Button
                  variant="primary"
                  size="sm"
                  disabled={email.trim().length === 0 || invite.isPending}
                  onClick={() =>
                    run(
                      () =>
                        invite
                          .mutateAsync({ email: email.trim(), role })
                          .then(() => setEmail("")),
                      "초대를 등록했습니다. 다만 초대 메일은 아직 발송되지 않습니다.",
                    )
                  }
                >
                  팀원 초대
                </Button>
              </>
            ) : null
          }
        />

        {notice ? (
          <Notice tone={notice.tone} className="mx-4.5 mt-3.5">
            {notice.text}
          </Notice>
        ) : null}

        {members.isPending ? (
          <LoadingState />
        ) : members.isError ? (
          <ErrorState message="팀원을 불러오지 못했습니다" onRetry={() => void members.refetch()} />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr>
                  <Th>이름</Th>
                  <Th>이메일</Th>
                  <Th className="w-[150px]">권한</Th>
                  <Th className="w-[110px]">마지막 접속</Th>
                  <Th className="w-[80px]" />
                </tr>
              </thead>
              <tbody>
                {members.data.map((member) => (
                  <tr key={member.id} className="hover:bg-paper/60">
                    <td className="border-b border-line-2 px-3.5 py-3 text-[13.5px] font-medium">
                      {member.name ?? "—"}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3 font-mono text-[12px] text-slate">
                      {member.email}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3">
                      {member.role === "OWNER" ? (
                        <StatusBadge tone="idle" label="소유자" />
                      ) : member.inviteState === "PENDING" ? (
                        <StatusBadge tone="warn" label="수락 대기" />
                      ) : isOwner ? (
                        <select
                          aria-label={`${member.email} 권한`}
                          value={member.role}
                          disabled={changeRole.isPending}
                          onChange={(event) =>
                            run(
                              () =>
                                changeRole.mutateAsync({
                                  id: member.id,
                                  role: event.target.value as TenantMemberRole,
                                }),
                              "권한을 바꿨습니다.",
                            )
                          }
                          className="rounded-md border border-line px-2 py-1 text-[12px]"
                        >
                          <option value="EDITOR">편집</option>
                          <option value="VIEWER">보기만</option>
                        </select>
                      ) : (
                        <span className="text-[12.5px]">{MEMBER_ROLE_LABEL[member.role]}</span>
                      )}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3 font-mono text-[11.5px] text-slate-2">
                      {member.lastSeenAt ? member.lastSeenAt.slice(0, 10) : "—"}
                    </td>
                    <td className="border-b border-line-2 px-3.5 py-3 text-right">
                      {isOwner && member.role !== "OWNER" ? (
                        <Button
                          size="sm"
                          variant="danger"
                          disabled={remove.isPending}
                          onClick={() => run(() => remove.mutateAsync(member.id), "팀원을 삭제했습니다.")}
                        >
                          삭제
                        </Button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card>
        <CardHeader
          title="운영팀 접속 이력"
          aside={<Eyebrow>{history.data?.length ?? 0}건</Eyebrow>}
        />
        <CardBody>
          <p className="mb-3.5 text-[12.5px] leading-relaxed text-slate">
            문의 처리나 문제 재현을 위해 운영팀이 이 대시보드에 접속한 기록입니다. 접속에는 반드시
            사유가 필요하며, 이 기록은 지워지지 않습니다.
          </p>

          {history.isPending ? (
            <LoadingState label="이력을 불러오는 중" />
          ) : history.isError ? (
            <ErrorState message="이력을 불러오지 못했습니다" onRetry={() => void history.refetch()} />
          ) : history.data.length === 0 ? (
            <p className="text-[13px] text-slate-2">운영팀이 접속한 기록이 없습니다.</p>
          ) : (
            <ul className="space-y-2.5">
              {history.data.map((item) => (
                <li key={item.sessionId} className="flex flex-wrap items-center gap-3 text-[13px]">
                  <span className="tabular w-[130px] shrink-0 text-[11.5px] text-slate-2">
                    {item.startedAt.slice(0, 16).replace("T", " ")}
                  </span>
                  <span className="min-w-0 flex-1">{item.reason}</span>
                  <StatusBadge
                    tone={item.status === "ACTIVE" ? "warn" : "idle"}
                    label={item.status === "ACTIVE" ? "접속 중" : "종료됨"}
                  />
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </>
  );
}

function Th({ className, children }: { className?: string; children?: React.ReactNode }) {
  return (
    <th
      className={`border-b border-line-2 px-3.5 pb-2.5 text-left font-mono text-[10.5px] font-medium tracking-[0.09em] text-slate-2 uppercase ${className ?? ""}`}
    >
      {children}
    </th>
  );
}
