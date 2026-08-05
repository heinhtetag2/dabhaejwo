"use client";

import { useState } from "react";

import { useCostGuardQuery } from "@/entities/guard";
import {
  useChangeEmbeddingProvider,
  useProviderCredentialsQuery,
  useSaveProviderKey,
  type ProviderCredential,
} from "@/entities/provider";
import type { ProviderName } from "@/entities/usage";
import { Badge, type BadgeTone } from "@/shared/common/badge";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { Field, Select, TextInput } from "@/shared/common/field";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { can, useAuthStore } from "@/shared/lib/auth-store";
import { errorMessage } from "@/shared/lib/error-message";
import { dateTime } from "@/shared/lib/format";
import { Modal } from "@/shared/ui/modal";

const SOURCE_LABEL: Record<ProviderCredential["source"], { text: string; tone: BadgeTone }> = {
  CONSOLE: { text: "콘솔 등록", tone: "ok" },
  ENV: { text: "환경변수", tone: "warn" },
  NONE: { text: "미설정", tone: "idle" },
};

/**
 * 공급사 연결.
 *
 * 키를 콘솔에서 바꿀 수 있게 만든 이유는 **노출된 키를 재배포 없이 교체**하기 위해서다.
 * 등록된 키는 다시 볼 수 없다 — 마스킹된 힌트만 보이고 교체만 할 수 있다.
 */
export function ProviderCard() {
  const operator = useAuthStore((state) => state.operator);
  const credentials = useProviderCredentialsQuery();
  const guard = useCostGuardQuery();
  const [editing, setEditing] = useState<ProviderName | null>(null);

  if (!can(operator, "PROVIDER_CREDENTIAL_READ")) {
    return null;
  }
  if (credentials.isPending) {
    return (
      <Card className="mb-4">
        <LoadingState />
      </Card>
    );
  }
  if (credentials.isError) {
    return (
      <Card className="mb-4">
        <ErrorState
          message={errorMessage(credentials.error)}
          onRetry={() => void credentials.refetch()}
        />
      </Card>
    );
  }

  const writable = can(operator, "PROVIDER_CREDENTIAL_WRITE");

  return (
    <>
      <Card className="mb-4">
        <CardHeader title="공급사 연결" aside={<Eyebrow>API 키</Eyebrow>} />

        <Table>
          <thead>
            <tr>
              <Th>공급사</Th>
              <Th className="w-[140px]">키</Th>
              <Th className="w-[110px]">출처</Th>
              <Th className="w-[170px]">마지막 변경</Th>
              <Th className="w-[110px]" />
            </tr>
          </thead>
          <tbody>
            {credentials.data.map((row) => (
              <tr key={row.provider} className={row.configured ? "" : "text-slate-2"}>
                <Td>
                  <TitleCell
                    title={row.provider}
                    sub={row.enabled ? undefined : "사용 중지됨"}
                  />
                </Td>
                <Td className="tabular text-[12.5px]">{row.keyHint ?? "—"}</Td>
                <Td>
                  <Badge tone={SOURCE_LABEL[row.source].tone} dot={false}>
                    {SOURCE_LABEL[row.source].text}
                  </Badge>
                </Td>
                <Td className="tabular text-[12px] text-slate-2">
                  {row.updatedAt ? `${dateTime(row.updatedAt)} · ${row.updatedByName ?? ""}` : "—"}
                </Td>
                <Td>
                  {writable ? (
                    <Button size="sm" onClick={() => setEditing(row.provider)}>
                      {row.source === "CONSOLE" ? "키 교체" : "키 등록"}
                    </Button>
                  ) : null}
                </Td>
              </tr>
            ))}
          </tbody>
        </Table>

        <CardBody className="border-t border-line-2 text-[12.5px] leading-relaxed text-slate">
          등록한 키는 <b className="font-semibold text-ink">다시 볼 수 없습니다</b> — 암호화해
          저장하고 화면에는 앞뒤 몇 자만 보여줍니다. 노출됐다면 새 키를 발급해 여기서 교체하세요.
          재배포가 필요 없습니다.
          <br />
          <b className="font-semibold text-ink">환경변수</b>로 표시된 공급사는 아직 서버 설정의
          키로 돌고 있습니다. 콘솔에 등록하면 그 값이 우선합니다.
        </CardBody>
      </Card>

      {guard.data ? (
        <EmbeddingProviderCard current={guard.data.embeddingProvider} writable={writable} />
      ) : null}

      {editing ? (
        <KeyModal provider={editing} onClose={() => setEditing(null)} />
      ) : null}
    </>
  );
}

function KeyModal({ provider, onClose }: { provider: ProviderName; onClose: () => void }) {
  const [apiKey, setApiKey] = useState("");
  const [reason, setReason] = useState("");
  const save = useSaveProviderKey();

  return (
    <Modal
      open
      onOpenChange={(next) => !next && onClose()}
      title={`${provider} 키 등록`}
      description="저장하면 즉시 이 키로 호출합니다."
      warning="입력한 키는 저장 후 다시 볼 수 없습니다. 값을 확인하고 저장하세요."
      confirmLabel="저장"
      confirmDisabled={apiKey.trim().length < 8 || reason.trim().length === 0}
      pending={save.isPending}
      error={save.isError ? errorMessage(save.error) : null}
      onConfirm={() => save.mutate({ provider, apiKey, reason }, { onSuccess: onClose })}
    >
      <Field label="API 키">
        {(id) => (
          <TextInput
            id={id}
            // 어깨너머로 읽히지 않게 가린다. 붙여넣기는 그대로 된다.
            type="password"
            autoComplete="off"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder="AIza..."
          />
        )}
      </Field>
      <Field label="사유 (필수)" hint="감사 기록에 남습니다. 키 자체는 기록되지 않습니다.">
        {(id) => (
          <TextInput
            id={id}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="예: 노출된 키 폐기 후 재발급"
          />
        )}
      </Field>
    </Modal>
  );
}

/**
 * 임베딩 공급사.
 *
 * 안전장치 저장과 버튼을 나눈 것이 의도다 — 이 값을 바꾸면 이미 학습된 조각이 전부
 * 무효가 되고, 업체가 "다시 학습"을 눌러야 새 공급사로 다시 만들어진다.
 */
function EmbeddingProviderCard({
  current,
  writable,
}: {
  current: string;
  writable: boolean;
}) {
  const [next, setNext] = useState(current);
  const [reason, setReason] = useState("");
  const [confirming, setConfirming] = useState(false);
  const change = useChangeEmbeddingProvider();

  return (
    <>
      <Card className="mb-4">
        <CardHeader title="임베딩 공급사" aside={<Badge tone="warn">되돌리기 비쌈</Badge>} />
        <CardBody>
          <Field
            label="문서·질문을 임베딩할 공급사"
            hint="바꾸면 이미 학습된 조각이 전부 무효가 됩니다. 다른 모델이 만든 벡터끼리는 거리를 비교할 수 없습니다."
          >
            {(id) => (
              <Select
                id={id}
                className="max-w-[320px]"
                disabled={!writable}
                value={next}
                onChange={(e) => setNext(e.target.value)}
              >
                <option value="STUB">STUB (가짜 임베딩 — 검색 순위에 의미 없음)</option>
                <option value="GOOGLE">GOOGLE</option>
                <option value="ANTHROPIC">ANTHROPIC</option>
                <option value="OPENAI">OPENAI</option>
              </Select>
            )}
          </Field>

          {writable && next !== current ? (
            <Button variant="danger" onClick={() => setConfirming(true)}>
              공급사 바꾸기
            </Button>
          ) : null}

          {current === "STUB" ? (
            <p className="mt-3 text-[12.5px] leading-relaxed text-brick">
              지금은 가짜 임베딩입니다. 검색이 되는 것처럼 보여도 순위에 의미가 없습니다.
            </p>
          ) : null}
        </CardBody>
      </Card>

      {confirming ? (
        <Modal
          open
          onOpenChange={(open) => !open && setConfirming(false)}
          title="임베딩 공급사 변경"
          warning={
            <>
              이미 학습된 <b>모든 문서가 다시 학습 대상</b>이 됩니다. 업체가 지식 화면에서
              &ldquo;다시 학습&rdquo;을 눌러야 새 공급사로 만들어지며, 그때 임베딩 비용이 다시
              발생합니다. 그전까지 검색은 예전 벡터로 동작합니다.
            </>
          }
          confirmLabel="바꾸기"
          confirmVariant="danger"
          confirmDisabled={reason.trim().length === 0}
          pending={change.isPending}
          error={change.isError ? errorMessage(change.error) : null}
          onConfirm={() =>
            change.mutate(
              { provider: next as ProviderName, reason },
              { onSuccess: () => setConfirming(false) },
            )
          }
        >
          <Field label="사유 (필수)">
            {(id) => (
              <TextInput id={id} value={reason} onChange={(e) => setReason(e.target.value)} />
            )}
          </Field>
        </Modal>
      ) : null}
    </>
  );
}
