"use client";

import { useState } from "react";

import {
  useOperatorListQuery,
  useRolePermissionsQuery,
  type Operator,
} from "@/entities/operator";
import { Badge } from "@/shared/common/badge";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { useAuthStore } from "@/shared/lib/auth-store";
import { errorMessage } from "@/shared/lib/error-message";
import { relative } from "@/shared/lib/format";
import { PageHeader } from "@/widgets/page-header/page-header";

import {
  ActiveModal,
  CreateOperatorModal,
  EditOperatorModal,
  PasswordModal,
} from "./operator-modals";

type Dialog =
  | { kind: "CREATE" }
  | { kind: "EDIT" | "PASSWORD" | "ACTIVE"; operator: Operator }
  | null;

/**
 * 운영자 계정 관리.
 *
 * **삭제가 없다.** 감사 기록이 행위자를 FK 로 참조하고 3년 보존이라 지울 수 없다 —
 * 퇴사한 사람의 행적도 남아 있어야 한다. 비활성화가 그 자리를 대신하며,
 * 비활성 계정도 목록에 남긴다(숨기면 왜 로그인이 안 되는지 알 수 없다).
 */
export function OperatorsView() {
  const me = useAuthStore((state) => state.operator);
  const operators = useOperatorListQuery();
  const roles = useRolePermissionsQuery();
  const [dialog, setDialog] = useState<Dialog>(null);

  const close = () => setDialog(null);
  const roleLabel = (role: string) =>
    roles.data?.find((r) => r.role === role)?.label ?? role;

  return (
    <>
      <PageHeader
        title="관리자"
        description="운영 콘솔에 로그인하는 계정"
        actions={
          <Button
            variant="primary"
            disabled={!roles.data}
            onClick={() => setDialog({ kind: "CREATE" })}
          >
            운영자 등록
          </Button>
        }
      />

      <Card>
        <CardHeader
          title="운영자"
          aside={
            <Eyebrow>
              {operators.data
                ? `${operators.data.filter((o) => o.active).length}명 활성`
                : ""}
            </Eyebrow>
          }
        />

        {operators.isPending || roles.isPending ? <LoadingState /> : null}
        {operators.isError ? (
          <ErrorState
            message={errorMessage(operators.error)}
            onRetry={() => void operators.refetch()}
          />
        ) : null}
        {operators.data && operators.data.length === 0 ? (
          <EmptyState message="등록된 운영자가 없습니다" />
        ) : null}

        {operators.data && operators.data.length > 0 ? (
          <Table>
            <thead>
              <tr>
                <Th>운영자</Th>
                <Th className="w-[110px]">역할</Th>
                <Th className="w-[90px]">상태</Th>
                <Th className="w-[110px]">마지막 접속</Th>
                <Th className="w-[220px]" />
              </tr>
            </thead>
            <tbody>
              {operators.data.map((operator) => {
                const self = me?.id === operator.id;
                return (
                  <tr key={operator.id} className={operator.active ? "" : "text-slate-2"}>
                    <Td>
                      <TitleCell
                        title={
                          <>
                            {operator.name}
                            {self ? (
                              <span className="ml-1.5 text-[11px] text-slate-2">(나)</span>
                            ) : null}
                          </>
                        }
                        sub={operator.email}
                      />
                    </Td>
                    <Td>
                      <Badge tone={operator.role === "OPS_ADMIN" ? "info" : "idle"} dot={false}>
                        {roleLabel(operator.role)}
                      </Badge>
                    </Td>
                    <Td>
                      <Badge tone={operator.active ? "ok" : "idle"}>
                        {operator.active ? "활성" : "비활성"}
                      </Badge>
                    </Td>
                    <Td className="tabular text-[12.5px] text-slate-2">
                      {relative(operator.lastSeenAt)}
                    </Td>
                    <Td>
                      <div className="flex flex-wrap gap-1.5">
                        <Button size="sm" onClick={() => setDialog({ kind: "EDIT", operator })}>
                          수정
                        </Button>
                        <Button
                          size="sm"
                          onClick={() => setDialog({ kind: "PASSWORD", operator })}
                        >
                          비밀번호
                        </Button>
                        <Button
                          size="sm"
                          variant={operator.active ? "danger" : "default"}
                          // 자기 계정은 끌 수 없다. 서버도 막지만 누르지 못하게 해 둔다.
                          disabled={self && operator.active}
                          onClick={() => setDialog({ kind: "ACTIVE", operator })}
                        >
                          {operator.active ? "비활성화" : "복구"}
                        </Button>
                      </div>
                    </Td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        ) : null}

        <CardBody className="border-t border-line-2 text-[12.5px] leading-relaxed text-slate">
          운영자는 <b className="font-semibold text-ink">삭제하지 않고 비활성화</b>합니다.
          감사 기록·쿼터 증량·내부 메모가 행위자로 이 계정을 가리키고 그 기록은 3년 보존이라,
          지우면 &ldquo;누가 이 업체에 대리 접속했나&rdquo;의 답이 사라집니다.
        </CardBody>
      </Card>

      <RoleMatrixCard />

      {dialog?.kind === "CREATE" && roles.data ? (
        <CreateOperatorModal roles={roles.data} onClose={close} />
      ) : null}
      {dialog?.kind === "EDIT" && roles.data ? (
        <EditOperatorModal
          operator={dialog.operator}
          roles={roles.data}
          self={me?.id === dialog.operator.id}
          onClose={close}
        />
      ) : null}
      {dialog?.kind === "PASSWORD" ? (
        <PasswordModal operator={dialog.operator} onClose={close} />
      ) : null}
      {dialog?.kind === "ACTIVE" ? (
        <ActiveModal operator={dialog.operator} onClose={close} />
      ) : null}
    </>
  );
}

/**
 * 역할별 권한.
 *
 * 서버가 코드의 진실을 그대로 준다 — 화면에 표를 복제해 두면 권한이 바뀌어도
 * 여기는 옛 표를 보여주고, 운영자는 실제와 다른 권한을 믿게 된다.
 */
function RoleMatrixCard() {
  const roles = useRolePermissionsQuery();

  if (!roles.data) {
    return null;
  }

  return (
    <Card className="mt-4">
      <CardHeader title="역할별 권한" aside={<Eyebrow>서버 기준</Eyebrow>} />
      <Table>
        <thead>
          <tr>
            <Th className="w-[130px]">역할</Th>
            <Th className="w-[70px]">개수</Th>
            <Th>권한</Th>
          </tr>
        </thead>
        <tbody>
          {roles.data.map((role) => (
            <tr key={role.role}>
              <Td>
                <TitleCell title={role.label} sub={role.role} />
              </Td>
              <Td className="tabular text-[13px] text-slate">{role.permissions.length}</Td>
              <Td className="font-mono text-[11px] leading-relaxed break-all text-slate-2">
                {role.role === "OPS_ADMIN" ? "전체" : role.permissions.join(" · ")}
              </Td>
            </tr>
          ))}
        </tbody>
      </Table>
      <CardBody className="border-t border-line-2 text-[12.5px] leading-relaxed text-slate">
        권한은 <b className="font-semibold text-ink">역할로만</b> 정해집니다. 개인별로 권한을
        붙이거나 떼는 기능은 두지 않았습니다 — 열어두면 누군가 자기 계정에 감사 기록 열람을
        붙일 수 있고, 그러면 감사 체계 자체가 의미를 잃습니다. 매핑을 바꾸려면 배포가 필요합니다.
      </CardBody>
    </Card>
  );
}
