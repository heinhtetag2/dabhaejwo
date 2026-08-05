"use client";

import { useState } from "react";

import {
  useChangeTenantPlan,
  useChangeTenantStatus,
  useExtendTrial,
  useGrantQuota,
  useStartImpersonation,
  type TenantDetail,
} from "@/entities/tenant";
import { usePlanListQuery } from "@/entities/plan";
import { Button } from "@/shared/common/button";
import { Field, NumberInput, Select, TextInput } from "@/shared/common/field";
import { can, useAuthStore } from "@/shared/lib/auth-store";
import { errorMessage } from "@/shared/lib/error-message";
import { Modal } from "@/shared/ui/modal";

type ActionKind = "IMPERSONATE" | "PLAN" | "QUOTA" | "TRIAL" | "SUSPEND" | null;

/**
 * 업체 조치 버튼과 모달.
 *
 * 사유가 필요한 행위는 서버가 사유 없이는 거부한다 — 여기서 required 를 거는 것은
 * 왕복 한 번을 아끼는 UX 이지 검증이 아니다.
 *
 * 권한이 없는 버튼은 숨긴다. 이것도 UX 다 — 눌러 봐야 403 이다.
 */
export function TenantActions({ tenant }: { tenant: TenantDetail }) {
  const operator = useAuthStore((state) => state.operator);
  const [open, setOpen] = useState<ActionKind>(null);

  const close = () => setOpen(null);

  return (
    <>
      <div className="flex flex-wrap gap-[7px]">
        {can(operator, "TENANT_IMPERSONATE") ? (
          <Button variant="accent" size="sm" onClick={() => setOpen("IMPERSONATE")}>
            대리 로그인
          </Button>
        ) : null}
        {can(operator, "TENANT_PLAN_WRITE") ? (
          <Button size="sm" onClick={() => setOpen("PLAN")}>
            요금제 변경
          </Button>
        ) : null}
        {can(operator, "QUOTA_GRANT") ? (
          <Button size="sm" onClick={() => setOpen("QUOTA")}>
            쿼터 임시 증량
          </Button>
        ) : null}
        {can(operator, "TENANT_TRIAL_WRITE") && tenant.status === "TRIAL" ? (
          <Button size="sm" onClick={() => setOpen("TRIAL")}>
            체험 연장
          </Button>
        ) : null}
        {can(operator, "TENANT_STATUS_WRITE") ? (
          <Button variant="danger" size="sm" onClick={() => setOpen("SUSPEND")}>
            {tenant.status === "SUSPENDED" ? "정지 해제" : "일시정지"}
          </Button>
        ) : null}
      </div>

      <ImpersonateModal open={open === "IMPERSONATE"} onClose={close} tenant={tenant} />
      <PlanModal open={open === "PLAN"} onClose={close} tenant={tenant} />
      <QuotaModal open={open === "QUOTA"} onClose={close} tenant={tenant} />
      <TrialModal open={open === "TRIAL"} onClose={close} tenant={tenant} />
      <StatusModal open={open === "SUSPEND"} onClose={close} tenant={tenant} />
    </>
  );
}

interface ModalProps {
  open: boolean;
  onClose: () => void;
  tenant: TenantDetail;
}

function ImpersonateModal({ open, onClose, tenant }: ModalProps) {
  const [reason, setReason] = useState("");
  const [issued, setIssued] = useState<string | null>(null);
  const mutation = useStartImpersonation(tenant.id);

  return (
    <Modal
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          setReason("");
          setIssued(null);
          mutation.reset();
          onClose();
        }
      }}
      title="대리 로그인"
      warning={
        <>
          <b>{tenant.name}</b>의 대시보드에 그 업체 담당자로 들어갑니다. 대화 로그를 포함한 고객
          데이터가 모두 보입니다. <b>접속 기록은 감사 기록에 남고 지울 수 없습니다.</b>
        </>
      }
      confirmLabel="사유 남기고 접속"
      confirmVariant="accent"
      confirmDisabled={reason.trim().length === 0}
      pending={mutation.isPending}
      error={mutation.isError ? errorMessage(mutation.error) : null}
      onConfirm={() => {
        mutation.mutate(
          { reason },
          { onSuccess: (session) => setIssued(session.accessToken) },
        );
      }}
    >
      {issued ? (
        <p className="text-[13px] leading-relaxed text-slate">
          세션이 열렸습니다. 발급된 토큰은 <b>VIEWER 권한</b>이라 설정을 바꿀 수 없고 30분 뒤
          만료됩니다. 업체 대시보드에서 이 토큰으로 접속하세요.
        </p>
      ) : (
        <Field
          label="접속 사유"
          hint="업체에도 공개되는 문구입니다. 구체적으로 적어주세요."
        >
          {(id) => (
            <TextInput
              id={id}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="예: 문의 #482 — PDF 업로드 실패 재현"
            />
          )}
        </Field>
      )}
    </Modal>
  );
}

function PlanModal({ open, onClose, tenant }: ModalProps) {
  const { data: plans } = usePlanListQuery();
  const [planId, setPlanId] = useState(tenant.plan?.id ?? "");
  const [reason, setReason] = useState("");
  const mutation = useChangeTenantPlan(tenant.id);

  return (
    <Modal
      open={open}
      onOpenChange={(next) => !next && onClose()}
      title="요금제 변경"
      description="변경 즉시 청구액과 원가율 계산이 달라집니다."
      confirmLabel="변경"
      confirmDisabled={!planId || reason.trim().length === 0}
      pending={mutation.isPending}
      error={mutation.isError ? errorMessage(mutation.error) : null}
      onConfirm={() => mutation.mutate({ planId, reason }, { onSuccess: onClose })}
    >
      <Field label="요금제">
        {(id) => (
          <Select id={id} value={planId} onChange={(e) => setPlanId(e.target.value)}>
            <option value="">선택하세요</option>
            {(plans ?? []).map((plan) => (
              <option key={plan.id} value={plan.id}>
                {plan.name}
                {plan.sellable ? "" : " (판매 중단)"}
              </option>
            ))}
          </Select>
        )}
      </Field>
      <ReasonField value={reason} onChange={setReason} />
    </Modal>
  );
}

function QuotaModal({ open, onClose, tenant }: ModalProps) {
  const [convDelta, setConvDelta] = useState(0);
  const [docDelta, setDocDelta] = useState(0);
  const [reason, setReason] = useState("");
  const mutation = useGrantQuota(tenant.id);

  return (
    <Modal
      open={open}
      onOpenChange={(next) => !next && onClose()}
      title="쿼터 임시 증량"
      description="이번 달에만 적용되고 다음 달 자동 원복됩니다. 이력은 남습니다."
      confirmLabel="증량"
      confirmDisabled={(convDelta === 0 && docDelta === 0) || reason.trim().length === 0}
      pending={mutation.isPending}
      error={mutation.isError ? errorMessage(mutation.error) : null}
      onConfirm={() => mutation.mutate({ convDelta, docDelta, reason }, { onSuccess: onClose })}
    >
      <Field label="대화 증량" hint="잘못 넣었으면 음수로 한 건 더 넣어 되돌립니다.">
        {(id) => (
          <NumberInput
            id={id}
            value={convDelta}
            onChange={(e) => setConvDelta(Number(e.target.value))}
          />
        )}
      </Field>
      <Field label="문서 증량">
        {(id) => (
          <NumberInput
            id={id}
            value={docDelta}
            onChange={(e) => setDocDelta(Number(e.target.value))}
          />
        )}
      </Field>
      <ReasonField value={reason} onChange={setReason} />
    </Modal>
  );
}

function TrialModal({ open, onClose, tenant }: ModalProps) {
  const [days, setDays] = useState(7);
  const [reason, setReason] = useState("");
  const mutation = useExtendTrial(tenant.id);

  return (
    <Modal
      open={open}
      onOpenChange={(next) => !next && onClose()}
      title="체험 연장"
      confirmLabel="연장"
      confirmDisabled={days < 1}
      pending={mutation.isPending}
      error={mutation.isError ? errorMessage(mutation.error) : null}
      onConfirm={() => mutation.mutate({ days, reason }, { onSuccess: onClose })}
    >
      <Field label="연장 일수" hint="최대 90일. 그 이상은 사실상 영구 무료 계정이 됩니다.">
        {(id) => (
          <NumberInput
            id={id}
            min={1}
            max={90}
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
          />
        )}
      </Field>
      <ReasonField value={reason} onChange={setReason} required={false} />
    </Modal>
  );
}

function StatusModal({ open, onClose, tenant }: ModalProps) {
  const suspended = tenant.status === "SUSPENDED";
  const [reason, setReason] = useState("");
  const mutation = useChangeTenantStatus(tenant.id);

  return (
    <Modal
      open={open}
      onOpenChange={(next) => !next && onClose()}
      title={suspended ? "정지 해제" : "일시정지"}
      warning={
        suspended ? undefined : (
          <>
            챗봇이 즉시 응답을 멈춥니다. 방문자에게는 안내 메시지만 표시됩니다.
          </>
        )
      }
      confirmLabel={suspended ? "해제" : "일시정지"}
      confirmVariant={suspended ? "primary" : "danger"}
      confirmDisabled={!suspended && reason.trim().length === 0}
      pending={mutation.isPending}
      error={mutation.isError ? errorMessage(mutation.error) : null}
      onConfirm={() =>
        mutation.mutate(
          { status: suspended ? "ACTIVE" : "SUSPENDED", reason },
          { onSuccess: onClose },
        )
      }
    >
      <ReasonField value={reason} onChange={setReason} required={!suspended} />
    </Modal>
  );
}

function ReasonField({
  value,
  onChange,
  required = true,
}: {
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
}) {
  return (
    <Field
      label={required ? "사유 (필수)" : "사유"}
      hint={required ? "감사 기록에 남습니다. 공백만 입력하면 거부됩니다." : "감사 기록에 남습니다."}
    >
      {(id) => (
        <TextInput id={id} value={value} onChange={(e) => onChange(e.target.value)} />
      )}
    </Field>
  );
}
