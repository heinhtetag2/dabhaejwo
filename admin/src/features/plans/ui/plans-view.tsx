"use client";

import * as Switch from "@radix-ui/react-switch";
import { useState } from "react";

import { useCostGuardQuery, useUpdateCostGuard, type QuotaExceededBehavior } from "@/entities/guard";
import { usePlanListQuery, useUpdatePlan, type Plan } from "@/entities/plan";
import { Card, CardBody, CardHeader } from "@/shared/common/card";
import { Field, Select, TextInput } from "@/shared/common/field";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th } from "@/shared/common/table";
import { can, useAuthStore } from "@/shared/lib/auth-store";
import { errorMessage } from "@/shared/lib/error-message";
import { count, won } from "@/shared/lib/format";
import { Modal } from "@/shared/ui/modal";
import { PageHeader } from "@/widgets/page-header/page-header";

const BEHAVIOR_LABEL: Record<QuotaExceededBehavior, string> = {
  STOP_AND_NOTICE: "챗봇을 멈추고 안내 메시지 표시",
  OVERAGE_BILLING: "초과분 과금",
  NOTIFY_ONLY: "그대로 두고 알림만",
};

export function PlansView() {
  const operator = useAuthStore((state) => state.operator);
  const plans = usePlanListQuery();
  const [editing, setEditing] = useState<Plan | null>(null);

  const writable = can(operator, "PLAN_WRITE");

  return (
    <>
      <PageHeader title="요금제" description="플랜 정의와 한도 정책" />

      <Card>
        <CardHeader title="요금제" />

        {plans.isPending ? <LoadingState /> : null}
        {plans.isError ? (
          <ErrorState
            message={errorMessage(plans.error)}
            onRetry={() => void plans.refetch()}
          />
        ) : null}
        {plans.data && plans.data.length === 0 ? (
          <EmptyState message="등록된 요금제가 없습니다" />
        ) : null}

        {plans.data && plans.data.length > 0 ? (
          <Table>
            <thead>
              <tr>
                <Th>이름</Th>
                <Th className="w-[100px]">월 요금</Th>
                <Th className="w-[92px]">대화 한도</Th>
                <Th className="w-[92px]">문서 한도</Th>
                <Th className="w-[80px]">사용 업체</Th>
                <Th className="w-[74px]">판매</Th>
              </tr>
            </thead>
            <tbody>
              {plans.data.map((plan) => (
                <tr key={plan.id} className={plan.sellable ? "" : "text-slate-2"}>
                  <Td>
                    <button
                      type="button"
                      className="text-left font-medium disabled:cursor-default"
                      disabled={!writable}
                      onClick={() => setEditing(plan)}
                    >
                      {plan.name}
                    </button>
                  </Td>
                  <Td className="tabular">
                    {plan.negotiable ? "협의" : won(plan.monthlyFee)}
                  </Td>
                  <Td className="tabular">{limitLabel(plan.convLimit)}</Td>
                  <Td className="tabular">{limitLabel(plan.docLimit)}</Td>
                  <Td className="tabular text-slate-2">{count(plan.tenantCount)}</Td>
                  <Td>
                    {/* 요금제는 삭제하지 않는다. 판매 중단만 한다 — 기존 계약 업체가 남아 있다 */}
                    <SellableSwitch plan={plan} disabled={!writable} />
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : null}

        <CardBody className="border-t border-line-2 text-[12.5px] leading-relaxed text-slate">
          요금제는 <b className="font-semibold text-ink">삭제하지 않고 판매 중단만</b> 합니다.
          기존 계약 업체가 남아 있고, 그 업체의 요금제가 사라지면 청구액을 계산할 수 없습니다.
        </CardBody>
      </Card>

      <QuotaPolicyCard />

      {editing ? (
        <PlanEditModal plan={editing} onClose={() => setEditing(null)} />
      ) : null}
    </>
  );
}

function limitLabel(value: number): string {
  return value >= 999_999 ? "무제한" : count(value);
}

function SellableSwitch({ plan, disabled }: { plan: Plan; disabled: boolean }) {
  const update = useUpdatePlan();
  return (
    <Switch.Root
      checked={plan.sellable}
      disabled={disabled || update.isPending}
      aria-label={`${plan.name} ${plan.sellable ? "판매 중단" : "판매 재개"}`}
      onCheckedChange={(next) =>
        update.mutate({
          planId: plan.id,
          body: {
            name: plan.name,
            monthlyFee: plan.monthlyFee,
            negotiable: plan.negotiable,
            convLimit: plan.convLimit,
            docLimit: plan.docLimit,
            sellable: next,
          },
        })
      }
      className="relative h-[19px] w-[34px] rounded-full bg-line transition-colors data-[state=checked]:bg-seal disabled:opacity-50"
    >
      <Switch.Thumb className="block size-[15px] translate-x-0.5 rounded-full bg-white shadow transition-transform data-[state=checked]:translate-x-[17px]" />
    </Switch.Root>
  );
}

function PlanEditModal({ plan, onClose }: { plan: Plan; onClose: () => void }) {
  const [name, setName] = useState(plan.name);
  const [monthlyFee, setMonthlyFee] = useState(String(plan.monthlyFee));
  const [convLimit, setConvLimit] = useState(String(plan.convLimit));
  const [docLimit, setDocLimit] = useState(String(plan.docLimit));
  const update = useUpdatePlan();

  return (
    <Modal
      open
      onOpenChange={(next) => !next && onClose()}
      title={`${plan.name} 수정`}
      description={`코드 ${plan.code} 는 바꿀 수 없습니다 — 코드가 이 값으로 요금제를 찾습니다.`}
      confirmLabel="저장"
      pending={update.isPending}
      error={update.isError ? errorMessage(update.error) : null}
      onConfirm={() =>
        update.mutate(
          {
            planId: plan.id,
            body: {
              name,
              monthlyFee: Number(monthlyFee),
              negotiable: plan.negotiable,
              convLimit: Number(convLimit),
              docLimit: Number(docLimit),
              sellable: plan.sellable,
            },
          },
          { onSuccess: onClose },
        )
      }
    >
      <Field label="이름">
        {(id) => <TextInput id={id} value={name} onChange={(e) => setName(e.target.value)} />}
      </Field>
      <Field label="월 요금 (원)" hint={plan.negotiable ? "협의가 요금제입니다." : undefined}>
        {(id) => (
          <TextInput
            id={id}
            inputMode="numeric"
            value={monthlyFee}
            onChange={(e) => setMonthlyFee(e.target.value)}
          />
        )}
      </Field>
      <Field label="대화 한도">
        {(id) => (
          <TextInput
            id={id}
            inputMode="numeric"
            value={convLimit}
            onChange={(e) => setConvLimit(e.target.value)}
          />
        )}
      </Field>
      <Field label="문서 한도">
        {(id) => (
          <TextInput
            id={id}
            inputMode="numeric"
            value={docLimit}
            onChange={(e) => setDocLimit(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/** 한도 초과 시 동작. 값은 cost_guards 에 있고 모델·프롬프트 화면과 같은 리소스다. */
function QuotaPolicyCard() {
  const operator = useAuthStore((state) => state.operator);
  const guard = useCostGuardQuery();
  const update = useUpdateCostGuard();
  const [reason, setReason] = useState("");

  if (!can(operator, "COST_GUARD_READ")) {
    return null;
  }
  if (guard.isPending) {
    return (
      <Card className="mt-4">
        <LoadingState />
      </Card>
    );
  }
  if (guard.isError) {
    return (
      <Card className="mt-4">
        <ErrorState message={errorMessage(guard.error)} onRetry={() => void guard.refetch()} />
      </Card>
    );
  }

  const current = guard.data;
  const writable = can(operator, "COST_GUARD_WRITE");

  return (
    <Card className="mt-4">
      <CardHeader title="한도를 넘겼을 때" />
      <CardBody>
        <Field
          label="대화 한도 초과 시"
          hint="초기 권장은 '챗봇을 멈추고 안내 표시'입니다 — 원가가 예측 가능해집니다."
        >
          {(id) => (
            <Select
              id={id}
              className="max-w-[320px]"
              disabled={!writable || update.isPending}
              value={current.quotaExceededBehavior}
              onChange={(e) =>
                update.mutate({
                  ...current,
                  quotaExceededBehavior: e.target.value as QuotaExceededBehavior,
                  reason: reason || "한도 초과 동작 변경",
                })
              }
            >
              {Object.entries(BEHAVIOR_LABEL).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          )}
        </Field>

        {writable ? (
          <Field label="변경 사유" hint="감사 기록에 남습니다.">
            {(id) => (
              <TextInput
                id={id}
                className="max-w-[320px]"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="예: 초과 과금 정책 도입"
              />
            )}
          </Field>
        ) : (
          <p className="text-[12.5px] text-slate-2">
            이 값은 운영 관리자만 바꿀 수 있습니다.
          </p>
        )}

        {update.isError ? (
          <p className="text-[12.5px] text-brick">{errorMessage(update.error)}</p>
        ) : null}
      </CardBody>
    </Card>
  );
}
