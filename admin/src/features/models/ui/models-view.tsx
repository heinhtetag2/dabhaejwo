"use client";

import * as Switch from "@radix-ui/react-switch";
import { useState } from "react";

import { useCostGuardQuery, useUpdateCostGuard, type CostGuard } from "@/entities/guard";
import { usePlanAssignmentsQuery, useSaveAssignment } from "@/entities/plan";
import {
  useModelPriceListQuery,
  useRegisterModelPrice,
  type ModelPrice,
  type PurposeKind,
} from "@/entities/pricing";
import type { ProviderName } from "@/entities/usage";
import { Badge } from "@/shared/common/badge";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { Field, NumberInput, Select, TextArea, TextInput } from "@/shared/common/field";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { can, useAuthStore } from "@/shared/lib/auth-store";
import { errorMessage } from "@/shared/lib/error-message";
import { count, dateTime } from "@/shared/lib/format";
import { Modal } from "@/shared/ui/modal";
import { PageHeader } from "@/widgets/page-header/page-header";

import { ProviderCard } from "./provider-card";

export function ModelsView() {
  const operator = useAuthStore((state) => state.operator);
  const guard = useCostGuardQuery();

  if (!can(operator, "MODEL_PRICE_READ")) {
    return (
      <>
        <PageHeader title="모델과 프롬프트" />
        <Card>
          <EmptyState message="이 화면은 운영 관리자만 볼 수 있습니다" />
        </Card>
      </>
    );
  }

  return (
    <>
      <PageHeader title="모델과 프롬프트" description="전 업체 공통 설정" />
      {/* 키가 없으면 아래 단가·배정이 전부 무의미하다. 그래서 맨 위에 둔다. */}
      <ProviderCard />
      <PriceCard />
      <AssignmentCard />
      {guard.isPending ? (
        <Card className="mt-4">
          <LoadingState />
        </Card>
      ) : guard.isError ? (
        <Card className="mt-4">
          <ErrorState message={errorMessage(guard.error)} onRetry={() => void guard.refetch()} />
        </Card>
      ) : (
        <GuardCards guard={guard.data} />
      )}
    </>
  );
}

/**
 * 모델 단가.
 *
 * **표 안에서 값을 고칠 수 없다.** 프로토타입은 `<input>` 으로 직접 수정하는 형태지만
 * 그대로 옮기면 소급 변경이 된다 — 단가는 이력이고, 기존 행을 고치면 그 단가로 이미
 * 계산된 과거 `ai_usage` 의 근거가 사라진다. 여기서는 "새 단가 등록"만 할 수 있고
 * 기존 행은 읽기 전용 이력이다 (admin-console-plan.md §4.7).
 */
function PriceCard() {
  const operator = useAuthStore((state) => state.operator);
  const prices = useModelPriceListQuery();
  const [open, setOpen] = useState(false);

  return (
    <Card className="mb-4">
      <CardHeader
        title="모델 단가"
        aside={
          <>
            <Eyebrow>100만 토큰당 · 원</Eyebrow>
            {can(operator, "MODEL_PRICE_WRITE") ? (
              <Button size="sm" variant="primary" onClick={() => setOpen(true)}>
                새 단가 등록
              </Button>
            ) : null}
          </>
        }
      />

      {prices.isPending ? <LoadingState /> : null}
      {prices.isError ? (
        <ErrorState message={errorMessage(prices.error)} onRetry={() => void prices.refetch()} />
      ) : null}
      {prices.data && prices.data.length === 0 ? (
        <EmptyState message="등록된 단가가 없습니다" />
      ) : null}

      {prices.data && prices.data.length > 0 ? (
        <Table>
          <thead>
            <tr>
              <Th>모델</Th>
              <Th className="w-[104px]">입력</Th>
              <Th className="w-[104px]">출력</Th>
              <Th className="w-[150px]">적용 시작</Th>
              <Th className="w-[84px]">적용 중</Th>
            </tr>
          </thead>
          <tbody>
            {prices.data.map((price) => (
              <PriceRow key={price.id} price={price} />
            ))}
          </tbody>
        </Table>
      ) : null}

      <CardBody className="border-t border-line-2 text-[12.5px] leading-relaxed text-slate">
        여기 적은 단가로 모든 원가와 원가율이 계산됩니다. 공급사가 가격을 바꾸거나 환율이 크게
        움직이면 <b className="font-semibold text-ink">새 행을 등록</b>하세요. 기존 행은 고칠 수
        없습니다 — 과거 기록은 그때의 단가로 이미 확정 저장돼 있고, 소급되면 지난달 정산이
        틀어집니다.
      </CardBody>

      {open ? <PriceRegisterModal onClose={() => setOpen(false)} /> : null}
    </Card>
  );
}

function PriceRow({ price }: { price: ModelPrice }) {
  return (
    <tr className={price.current ? "" : "text-slate-2"}>
      <Td>
        <TitleCell
          title={price.model}
          sub={`${price.provider} · ${price.purposeKind === "EMBED" ? "임베딩" : "답변 생성"}`}
        />
      </Td>
      <Td className="tabular">{count(price.inputPer1m)}</Td>
      <Td className="tabular">{price.outputPer1m === null ? "—" : count(price.outputPer1m)}</Td>
      <Td className="tabular text-[12.5px]">{dateTime(price.effectiveFrom)}</Td>
      <Td>{price.current ? <Badge tone="ok">적용 중</Badge> : <Badge tone="idle">이력</Badge>}</Td>
    </tr>
  );
}

function PriceRegisterModal({ onClose }: { onClose: () => void }) {
  const [provider, setProvider] = useState<ProviderName>("GOOGLE");
  const [model, setModel] = useState("");
  const [purposeKind, setPurposeKind] = useState<PurposeKind>("GENERATE");
  const [inputPer1m, setInputPer1m] = useState("");
  const [outputPer1m, setOutputPer1m] = useState("");
  const [effectiveFrom, setEffectiveFrom] = useState("");
  const [reason, setReason] = useState("");
  const register = useRegisterModelPrice();

  return (
    <Modal
      open
      onOpenChange={(next) => !next && onClose()}
      title="새 단가 등록"
      description="기존 행을 고치는 것이 아니라 새 행을 추가합니다."
      warning="과거 원가는 소급되지 않습니다. 이 단가는 적용 시작 시각 이후의 호출에만 쓰입니다."
      confirmLabel="등록"
      confirmDisabled={!model || !inputPer1m || reason.trim().length === 0}
      pending={register.isPending}
      error={register.isError ? errorMessage(register.error) : null}
      onConfirm={() =>
        register.mutate(
          {
            provider,
            model,
            purposeKind,
            inputPer1m: Number(inputPer1m),
            outputPer1m: purposeKind === "EMBED" ? null : Number(outputPer1m),
            effectiveFrom: effectiveFrom ? new Date(effectiveFrom).toISOString() : null,
            note: null,
            reason,
          },
          { onSuccess: onClose },
        )
      }
    >
      <Field label="공급사">
        {(id) => (
          <Select
            id={id}
            value={provider}
            onChange={(e) => setProvider(e.target.value as ProviderName)}
          >
            <option value="GOOGLE">GOOGLE</option>
            <option value="ANTHROPIC">ANTHROPIC</option>
            <option value="OPENAI">OPENAI</option>
            <option value="STUB">STUB</option>
          </Select>
        )}
      </Field>
      <Field label="모델">
        {(id) => (
          <TextInput
            id={id}
            value={model}
            onChange={(e) => setModel(e.target.value)}
            placeholder="gemini-3.5-flash"
          />
        )}
      </Field>
      <Field label="종류">
        {(id) => (
          <Select
            id={id}
            value={purposeKind}
            onChange={(e) => setPurposeKind(e.target.value as PurposeKind)}
          >
            <option value="GENERATE">답변 생성</option>
            <option value="EMBED">임베딩</option>
          </Select>
        )}
      </Field>
      <Field label="입력 단가 (100만 토큰당 원)">
        {(id) => (
          <NumberInput
            id={id}
            value={inputPer1m}
            onChange={(e) => setInputPer1m(e.target.value)}
          />
        )}
      </Field>
      {purposeKind === "GENERATE" ? (
        <Field label="출력 단가 (100만 토큰당 원)">
          {(id) => (
            <NumberInput
              id={id}
              value={outputPer1m}
              onChange={(e) => setOutputPer1m(e.target.value)}
            />
          )}
        </Field>
      ) : null}
      <Field
        label="적용 시작"
        hint="비우면 지금부터입니다. 미래를 넣으면 예약됩니다 — 인상 공지를 미리 넣어 둘 수 있습니다."
      >
        {(id) => (
          <TextInput
            id={id}
            type="datetime-local"
            value={effectiveFrom}
            onChange={(e) => setEffectiveFrom(e.target.value)}
          />
        )}
      </Field>
      <Field label="사유 (필수)" hint="단가 변경은 전체 원가 계산에 즉시 영향이 갑니다.">
        {(id) => (
          <TextInput id={id} value={reason} onChange={(e) => setReason(e.target.value)} />
        )}
      </Field>
    </Modal>
  );
}

function AssignmentCard() {
  const assignments = usePlanAssignmentsQuery();
  const save = useSaveAssignment();

  return (
    <Card className="mb-4">
      <CardHeader title="요금제별 모델 배정" />

      {assignments.isPending ? <LoadingState /> : null}
      {assignments.data && assignments.data.length === 0 ? (
        <EmptyState message="배정된 모델이 없습니다" />
      ) : null}

      {assignments.data && assignments.data.length > 0 ? (
        <Table>
          <thead>
            <tr>
              <Th className="w-[120px]">요금제</Th>
              <Th>답변 생성</Th>
              <Th className="w-[120px]">조각 수</Th>
              <Th className="w-[180px]">예상 대화당 원가</Th>
            </tr>
          </thead>
          <tbody>
            {assignments.data.map((row) => (
              <tr key={row.plan.id}>
                <Td className="font-medium">{row.plan.name}</Td>
                <Td>
                  <TitleCell title={row.model} sub={row.provider} />
                </Td>
                <Td>
                  <NumberInput
                    aria-label={`${row.plan.name} 조각 수`}
                    min={1}
                    max={20}
                    defaultValue={row.chunkCount}
                    disabled={save.isPending}
                    className="w-20 px-2 py-1 text-[12.5px]"
                    onBlur={(e) => {
                      const next = Number(e.target.value);
                      if (next !== row.chunkCount) {
                        save.mutate({
                          planId: row.plan.id,
                          provider: row.provider,
                          model: row.model,
                          chunkCount: next,
                        });
                      }
                    }}
                  />
                </Td>
                <Td className="tabular text-[13px] text-slate">
                  {row.estimatedCostPerConvKrw === null
                    ? "단가 미등록"
                    : `약 ${row.estimatedCostPerConvKrw}원`}
                </Td>
              </tr>
            ))}
          </tbody>
        </Table>
      ) : null}

      {save.isError ? (
        <CardBody className="py-3">
          <p className="text-[12.5px] text-brick">{errorMessage(save.error)}</p>
        </CardBody>
      ) : null}

      <CardBody className="border-t border-line-2 text-[12.5px] leading-relaxed text-slate">
        맨 오른쪽은 현재 단가와 조각 수로 <b className="font-semibold text-ink">자동 계산한 추정치</b>
        입니다. 요금제 가격을 정할 때 이 숫자의 3배 이상은 받아야 서버비와 인건비가 남습니다.
        조각 수를 늘리면 입력 토큰이 비례해 늘어납니다 — 답변 원가의 대부분은 출력이 아니라
        입력입니다.
      </CardBody>
    </Card>
  );
}

/** 비용 안전장치 + 검색 설정 + 공통 프롬프트. 전부 cost_guards 단일 행이다. */
function GuardCards({ guard }: { guard: CostGuard }) {
  const operator = useAuthStore((state) => state.operator);
  const update = useUpdateCostGuard();
  const writable = can(operator, "COST_GUARD_WRITE");

  const [form, setForm] = useState({ ...guard, reason: "" });

  function set<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <>
      <Card className="mb-4">
        <CardHeader title="비용 안전장치" aside={<Badge tone="warn">중요</Badge>} />
        <CardBody>
          <div className="grid gap-x-4 sm:grid-cols-2">
            <Field
              label="업체별 하루 원가 상한 (원)"
              hint="넘으면 그 업체의 챗봇이 안내 메시지만 띄우고 멈춥니다."
            >
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.tenantDailyCapKrw}
                  onChange={(e) => set("tenantDailyCapKrw", Number(e.target.value))}
                />
              )}
            </Field>
            <Field
              label="전체 하루 원가 상한 (원)"
              hint="공격이나 버그로 폭주할 때 마지막 방어선입니다."
            >
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.globalDailyCapKrw}
                  onChange={(e) => set("globalDailyCapKrw", Number(e.target.value))}
                />
              )}
            </Field>
            <Field label="같은 IP 분당 질문 수" hint="스크립트로 챗봇을 긁어가는 것을 막습니다.">
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.ipQuestionsPerMin}
                  onChange={(e) => set("ipQuestionsPerMin", Number(e.target.value))}
                />
              )}
            </Field>
            <Field
              label="한 번에 올릴 수 있는 문서"
              hint="대량 업로드로 임베딩 비용이 한꺼번에 터지는 걸 막습니다."
            >
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.bulkUploadLimit}
                  onChange={(e) => set("bulkUploadLimit", Number(e.target.value))}
                />
              )}
            </Field>
            <Field
              label="원가율 경고선 (%)"
              hint="이 값을 넘으면 목록의 막대가 노랑으로 바뀝니다."
            >
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.costRatioWarnPercent}
                  onChange={(e) => set("costRatioWarnPercent", Number(e.target.value))}
                />
              )}
            </Field>
            <Field label="해지 후 데이터 보존 (일)" hint="이 기간이 지나면 벡터·문서를 지웁니다.">
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.churnPurgeGraceDays}
                  onChange={(e) => set("churnPurgeGraceDays", Number(e.target.value))}
                />
              )}
            </Field>
          </div>

          <div className="flex items-center gap-2.5 border-t border-line-2 pt-3.5">
            <Switch.Root
              checked={form.slackAlertEnabled}
              disabled={!writable}
              aria-label="상한 도달 시 슬랙 알림"
              onCheckedChange={(next) => set("slackAlertEnabled", next)}
              className="relative h-[19px] w-[34px] rounded-full bg-line transition-colors data-[state=checked]:bg-seal"
            >
              <Switch.Thumb className="block size-[15px] translate-x-0.5 rounded-full bg-white shadow transition-transform data-[state=checked]:translate-x-[17px]" />
            </Switch.Root>
            <div className="text-[13.5px]">
              상한 도달 시 슬랙으로 즉시 알림
              {/* 슬랙은 아직 로그 stub 이다 (CLAUDE.md Stub 목록) */}
              <div className="text-[11.5px] text-slate-2">
                #ops-alert 채널 · 아직 연동 전이라 서버 로그에만 남습니다
              </div>
            </div>
          </div>
        </CardBody>
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader title="검색 설정" />
          <CardBody>
            <Field
              label="가져올 조각 수 (기본값)"
              hint="많을수록 정확하지만 입력 토큰이 비례해 늘어납니다. 권장 5~10."
            >
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.defaultChunkCount}
                  onChange={(e) => set("defaultChunkCount", Number(e.target.value))}
                />
              )}
            </Field>
            <Field
              label="답변 실패 판단 기준"
              hint="가장 가까운 조각의 유사도가 이 값보다 낮으면 답변 실패로 처리합니다."
            >
              {(id) => (
                <NumberInput
                  id={id}
                  step="0.001"
                  min={0}
                  max={1}
                  disabled={!writable}
                  value={form.answerFailSimilarity}
                  onChange={(e) => set("answerFailSimilarity", Number(e.target.value))}
                />
              )}
            </Field>
            <Field
              label="답변 최대 길이"
              hint="출력 토큰을 제한해 원가를 예측 가능하게 만듭니다."
            >
              {(id) => (
                <NumberInput
                  id={id}
                  disabled={!writable}
                  value={form.answerMaxLength}
                  onChange={(e) => set("answerMaxLength", Number(e.target.value))}
                />
              )}
            </Field>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="공통 프롬프트" aside={<Eyebrow>모든 업체에 적용</Eyebrow>} />
          <CardBody>
            <Field
              label="기본 규칙"
              hint="업체가 설정한 말투 앞에 붙습니다. 업체는 이 내용을 볼 수 없습니다."
            >
              {(id) => (
                <TextArea
                  id={id}
                  disabled={!writable}
                  className="min-h-[150px]"
                  value={form.commonPrompt}
                  onChange={(e) => set("commonPrompt", e.target.value)}
                />
              )}
            </Field>
          </CardBody>
        </Card>
      </div>

      {writable ? (
        <Card className="mt-4">
          <CardBody className="flex flex-wrap items-end gap-3">
            <div className="min-w-[240px] flex-1">
              <Field label="변경 사유 (필수)" hint="감사 기록에 남습니다.">
                {(id) => (
                  <TextInput
                    id={id}
                    value={form.reason}
                    onChange={(e) => set("reason", e.target.value)}
                    placeholder="예: 트래픽 증가로 전체 상한 상향"
                  />
                )}
              </Field>
            </div>
            <Button
              variant="primary"
              className="mb-4"
              disabled={form.reason.trim().length === 0 || update.isPending}
              onClick={() => update.mutate(form)}
            >
              저장
            </Button>
            {update.isError ? (
              <p className="mb-4 text-[12.5px] text-brick">{errorMessage(update.error)}</p>
            ) : null}
            {update.isSuccess ? (
              <p className="mb-4 text-[12.5px] text-seal">저장했습니다</p>
            ) : null}
          </CardBody>
        </Card>
      ) : null}
    </>
  );
}
