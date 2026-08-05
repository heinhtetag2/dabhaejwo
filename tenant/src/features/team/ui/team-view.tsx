"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  MEMBER_ROLE_LABEL,
  useChangeMemberRole,
  useImpersonationHistoryQuery,
  useInviteMember,
  useResendInvite,
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
import { controlClass } from "@/shared/common/control";

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
  const resend = useResendInvite();
  const changeRole = useChangeMemberRole();
  const remove = useRemoveMember();

  const [form, setForm] = useState({ name: "", email: "", phone: "" });
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
        <CardHeader title="팀원" />

        {isOwner ? (
          <form
            className="border-b border-line-2 px-4.5 py-4"
            onSubmit={(event) => {
              event.preventDefault();
              run(
                () =>
                  invite
                    .mutateAsync({
                      name: form.name.trim(),
                      email: form.email.trim(),
                      role,
                      phone: form.phone.trim() || undefined,
                    })
                    .then(() => setForm({ name: "", email: "", phone: "" })),
                // 서버는 메일이 나가야 성공한다. 그래서 성공 = 링크가 갔다는 뜻이다.
                "초대 메일을 보냈습니다. 상대가 링크에서 비밀번호를 정하면 바로 쓸 수 있습니다.",
              );
            }}
          >
            <div className="grid items-start gap-2.5 sm:grid-cols-[1fr_1.4fr_1fr_auto_auto]">
              <InviteInput
                id="invite-name"
                label="이름"
                value={form.name}
                onChange={(value) => setForm((prev) => ({ ...prev, name: value }))}
                placeholder="이름"
                required
              />
              <InviteInput
                id="invite-email"
                label="이메일"
                type="email"
                value={form.email}
                onChange={(value) => setForm((prev) => ({ ...prev, email: value }))}
                placeholder="name@example.com"
                required
              />
              <InviteInput
                id="invite-phone"
                label="전화번호"
                value={form.phone}
                onChange={(value) => setForm((prev) => ({ ...prev, phone: value }))}
                placeholder="010-0000-0000 (선택)"
              />
              <div>
                <label className="sr-only" htmlFor="invite-role">
                  권한
                </label>
                <select
                  id="invite-role"
                  value={role}
                  onChange={(event) => setRole(event.target.value as TenantMemberRole)}
                  className={controlClass("md")}
                >
                  <option value="EDITOR">편집</option>
                  <option value="VIEWER">보기만</option>
                </select>
              </div>
              <Button
                type="submit"
                variant="primary"
                disabled={
                  form.name.trim().length === 0 ||
                  form.email.trim().length === 0 ||
                  invite.isPending
                }
              >
                {invite.isPending ? "보내는 중…" : "초대 메일 보내기"}
              </Button>
            </div>
            <p className="mt-2.5 text-[11.5px] leading-relaxed text-slate-2">
              초대받은 분은 메일의 링크에서 비밀번호를 정합니다. 링크는 7일 동안 유효하며 한 번만
              쓸 수 있습니다.
            </p>
          </form>
        ) : null}

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
                  <Th className="w-[150px]" />
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
                    <td className="border-b border-line-2 px-3.5 py-3">
                      {/* 버튼이 접히지 않게 한 줄로 못 박는다. 칸이 좁으면 표가 가로로 스크롤된다. */}
                      <div className="flex items-center justify-end gap-1.5 whitespace-nowrap">
                      {isOwner && member.inviteState === "PENDING" ? (
                        <Button
                          size="sm"
                          disabled={resend.isPending}
                          onClick={() =>
                            run(
                              () => resend.mutateAsync(member.id),
                              "초대 메일을 다시 보냈습니다. 이전 링크는 더 이상 쓸 수 없습니다.",
                            )
                          }
                        >
                          다시 보내기
                        </Button>
                      ) : null}
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
                      </div>
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

/** 초대 폼의 입력 한 칸. 라벨은 화면에 두지 않고 스크린 리더에만 준다 — 한 줄에 넣기 위해서다. */
function InviteInput({
  id,
  label,
  value,
  onChange,
  placeholder,
  type = "text",
  required,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: string;
  required?: boolean;
}) {
  return (
    <div>
      <label className="sr-only" htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        required={required}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        className={controlClass("md")}
      />
    </div>
  );
}
