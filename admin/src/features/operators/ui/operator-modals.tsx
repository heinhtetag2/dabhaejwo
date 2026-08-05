"use client";

import { useState } from "react";

import {
  useChangeOperatorActive,
  useCreateOperator,
  useResetOperatorPassword,
  useUpdateOperator,
  type Operator,
  type RolePermissions,
} from "@/entities/operator";
import { Field, Select, TextInput } from "@/shared/common/field";
import type { OperatorRole } from "@/shared/lib/auth-store";
import { errorMessage } from "@/shared/lib/error-message";
import { Modal } from "@/shared/ui/modal";

/** 역할 선택 + 그 역할이 무엇을 할 수 있는지. 고르는 순간 결과가 보여야 한다. */
function RoleField({
  value,
  onChange,
  roles,
  disabled,
}: {
  value: OperatorRole;
  onChange: (role: OperatorRole) => void;
  roles: RolePermissions[];
  disabled?: boolean;
}) {
  const selected = roles.find((r) => r.role === value);

  return (
    <>
      <Field label="역할">
        {(id) => (
          <Select
            id={id}
            value={value}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value as OperatorRole)}
          >
            {roles.map((role) => (
              <option key={role.role} value={role.role}>
                {role.label}
              </option>
            ))}
          </Select>
        )}
      </Field>

      {selected ? (
        <div className="mb-4 rounded-lg bg-line-2/50 px-3 py-2.5">
          <p className="mb-1.5 text-[11.5px] font-medium text-slate">
            {selected.label}이(가) 할 수 있는 것 · {selected.permissions.length}개
          </p>
          <p className="font-mono text-[11px] leading-relaxed break-all text-slate-2">
            {selected.role === "OPS_ADMIN"
              ? "전체 — 운영자 계정 관리·감사 기록·모델 단가·안전장치를 포함한 모든 권한"
              : selected.permissions.join(" · ")}
          </p>
        </div>
      ) : null}
    </>
  );
}

export function CreateOperatorModal({
  roles,
  onClose,
}: {
  roles: RolePermissions[];
  onClose: () => void;
}) {
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState<OperatorRole>("CS");
  const [password, setPassword] = useState("");
  const [reason, setReason] = useState("");
  const create = useCreateOperator();

  return (
    <Modal
      open
      onOpenChange={(next) => !next && onClose()}
      title="운영자 등록"
      description="등록 즉시 로그인할 수 있습니다."
      confirmLabel="등록"
      confirmDisabled={
        !email.trim() || !name.trim() || password.length < 8 || !reason.trim()
      }
      pending={create.isPending}
      error={create.isError ? errorMessage(create.error) : null}
      onConfirm={() =>
        create.mutate({ email, name, role, password, reason }, { onSuccess: onClose })
      }
    >
      <Field label="이메일" hint="로그인 식별자이며 등록 후에는 바꿀 수 없습니다.">
        {(id) => (
          <TextInput
            id={id}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="name@dabhaejwo.com"
          />
        )}
      </Field>
      <Field label="이름">
        {(id) => <TextInput id={id} value={name} onChange={(e) => setName(e.target.value)} />}
      </Field>

      <RoleField value={role} onChange={setRole} roles={roles} />

      <Field
        label="초기 비밀번호"
        hint="8자 이상. 초대 메일이 아직 연결되지 않아 직접 정해 전달해야 합니다."
      >
        {(id) => (
          <TextInput
            id={id}
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        )}
      </Field>
      <ReasonField value={reason} onChange={setReason} />
    </Modal>
  );
}

export function EditOperatorModal({
  operator,
  roles,
  self,
  onClose,
}: {
  operator: Operator;
  roles: RolePermissions[];
  self: boolean;
  onClose: () => void;
}) {
  const [name, setName] = useState(operator.name);
  const [role, setRole] = useState<OperatorRole>(operator.role);
  const [reason, setReason] = useState("");
  const update = useUpdateOperator();

  return (
    <Modal
      open
      onOpenChange={(next) => !next && onClose()}
      title={`${operator.email} 수정`}
      description="이메일은 바꿀 수 없습니다 — 감사 기록이 가리키는 사람입니다."
      confirmLabel="저장"
      confirmDisabled={!name.trim() || !reason.trim()}
      pending={update.isPending}
      error={update.isError ? errorMessage(update.error) : null}
      onConfirm={() =>
        update.mutate({ id: operator.id, body: { name, role, reason } }, { onSuccess: onClose })
      }
    >
      <Field label="이름">
        {(id) => <TextInput id={id} value={name} onChange={(e) => setName(e.target.value)} />}
      </Field>

      {/* 자기 역할을 낮추면 그 순간 이 화면에서 쫓겨난다. 서버도 막지만 미리 알려준다. */}
      <RoleField value={role} onChange={setRole} roles={roles} disabled={self} />
      {self ? (
        <p className="mb-4 -mt-2 text-[11.5px] text-slate-2">
          자기 역할은 바꿀 수 없습니다. 다른 운영 관리자에게 요청하세요.
        </p>
      ) : null}

      <ReasonField value={reason} onChange={setReason} />
    </Modal>
  );
}

export function PasswordModal({
  operator,
  onClose,
}: {
  operator: Operator;
  onClose: () => void;
}) {
  const [password, setPassword] = useState("");
  const [reason, setReason] = useState("");
  const reset = useResetOperatorPassword();

  return (
    <Modal
      open
      onOpenChange={(next) => !next && onClose()}
      title="비밀번호 재설정"
      warning={
        <>
          <b>{operator.name}</b> 님의 비밀번호를 바꿉니다. 기존 비밀번호는 확인하지 않으므로
          이 조작은 감사 기록에 남습니다.
        </>
      }
      confirmLabel="재설정"
      confirmDisabled={password.length < 8 || !reason.trim()}
      pending={reset.isPending}
      error={reset.isError ? errorMessage(reset.error) : null}
      onConfirm={() =>
        reset.mutate({ id: operator.id, password, reason }, { onSuccess: onClose })
      }
    >
      <Field label="새 비밀번호" hint="8자 이상. 본인에게 직접 전달하세요.">
        {(id) => (
          <TextInput
            id={id}
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        )}
      </Field>
      <ReasonField value={reason} onChange={setReason} />
    </Modal>
  );
}

export function ActiveModal({
  operator,
  onClose,
}: {
  operator: Operator;
  onClose: () => void;
}) {
  const [reason, setReason] = useState("");
  const change = useChangeOperatorActive();
  const deactivating = operator.active;

  return (
    <Modal
      open
      onOpenChange={(next) => !next && onClose()}
      title={deactivating ? "계정 비활성화" : "계정 복구"}
      warning={
        deactivating ? (
          <>
            <b>{operator.name}</b> 님이 더 이상 로그인할 수 없게 됩니다.
            <br />
            <b>계정을 지우는 것이 아닙니다</b> — 지난 감사 기록에는 이름이 그대로 남습니다.
            운영자를 삭제하면 &ldquo;누가 이 업체에 접속했나&rdquo;의 답이 사라지므로 삭제는
            아예 제공하지 않습니다.
          </>
        ) : undefined
      }
      confirmLabel={deactivating ? "비활성화" : "복구"}
      confirmVariant={deactivating ? "danger" : "primary"}
      confirmDisabled={!reason.trim()}
      pending={change.isPending}
      error={change.isError ? errorMessage(change.error) : null}
      onConfirm={() =>
        change.mutate(
          { id: operator.id, active: !operator.active, reason },
          { onSuccess: onClose },
        )
      }
    >
      <ReasonField value={reason} onChange={setReason} />
    </Modal>
  );
}

function ReasonField({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <Field label="사유 (필수)" hint="감사 기록에 남습니다. 비밀번호는 기록되지 않습니다.">
      {(id) => <TextInput id={id} value={value} onChange={(e) => onChange(e.target.value)} />}
    </Field>
  );
}
