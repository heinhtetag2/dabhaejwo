"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  useBotSettingsQuery,
  useSaveBotSettings,
  type BotSettings,
} from "@/entities/chatbot/bot-settings";
import { useFaqListQuery } from "@/entities/chatbot/faq";
import { ApiError } from "@/shared/api/http-client";
import { Button, LinkButton } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { ROUTES } from "@/shared/config/routes";
import { canEdit } from "@/shared/lib/auth-store";
import { Notice } from "@/shared/ui/notice";
import { WidgetPreview } from "@/shared/ui/widget-preview";

const PRESET_COLORS = ["#17222E", "#1B6B5C", "#BF3F2B", "#3B5BDB"];

/**
 * 말투와 모양.
 *
 * <p>설정을 바꾸면 오른쪽 미리보기에 즉시 반영된다. 저장 전에 눈으로 확인할 수 있어야
 * 업체가 지식을 채울 동기가 생긴다 (tenant-plan.md §4.7).
 *
 * <p>채팅창에 뜨는 질문 문구는 <b>공통 질문에서만</b> 관리한다. 같은 값을 두 화면에서
 * 고칠 수 있으면 반드시 어긋난다 (§2.4).
 */
export function AppearanceView() {
  const { data: context } = useAppContextQuery();
  const { data: saved, isPending, isError, refetch } = useBotSettingsQuery();
  const { data: faqs } = useFaqListQuery();
  const save = useSaveBotSettings();

  /**
   * 편집 중인 값만 따로 든다. 서버 값을 상태로 복사해 두면 다시 불러왔을 때 어긋나므로,
   * 손대기 전에는 서버 값을 그대로 쓰고 손댄 뒤에만 로컬 값이 이긴다.
   */
  const [localDraft, setLocalDraft] = useState<BotSettings | null>(null);
  const [error, setError] = useState<string | null>(null);

  const editable = canEdit(context?.member?.role);
  const shownFaqs = (faqs ?? []).filter((faq) => faq.shown);
  const draft = localDraft ?? saved;

  if (isPending || draft === undefined) {
    return <LoadingState />;
  }
  if (isError) {
    return <ErrorState message="설정을 불러오지 못했습니다" onRetry={() => void refetch()} />;
  }

  const patch = (changes: Partial<BotSettings>) => setLocalDraft({ ...draft, ...changes });

  const submit = () => {
    setError(null);
    save.mutate(draft, {
      // 저장이 끝나면 서버 값이 진실이다. 로컬 편집본을 버려 다음 조회 결과를 그대로 쓴다.
      onSuccess: () => setLocalDraft(null),
      onError: (cause: unknown) =>
        setError(cause instanceof ApiError ? cause.message : "저장하지 못했습니다"),
    });
  };

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
      <div>
        <Card className="mb-4">
          <CardHeader title="모양" />
          <CardBody>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field id="bot-name" label="챗봇 이름">
                <input
                  id="bot-name"
                  value={draft.botName}
                  disabled={!editable}
                  onChange={(event) => patch({ botName: event.target.value })}
                  className={INPUT}
                />
              </Field>

              <Field id="brand-color" label="버블 색상">
                <span className="flex flex-wrap items-center gap-2">
                  {PRESET_COLORS.map((color) => (
                    <button
                      key={color}
                      type="button"
                      aria-label={`색상 ${color}`}
                      aria-pressed={draft.brandColor.toUpperCase() === color}
                      disabled={!editable}
                      onClick={() => patch({ brandColor: color })}
                      className="size-[30px] rounded-[7px] border-2"
                      style={{
                        backgroundColor: color,
                        borderColor:
                          draft.brandColor.toUpperCase() === color ? "#17222E" : "transparent",
                      }}
                    />
                  ))}
                  <input
                    id="brand-color"
                    value={draft.brandColor}
                    disabled={!editable}
                    onChange={(event) => patch({ brandColor: event.target.value })}
                    className="w-[96px] rounded-[7px] border border-line px-2.5 py-1.5 font-mono text-[12px]"
                  />
                </span>
              </Field>
            </div>

            <Field id="greeting" label="첫 인사말">
              <input
                id="greeting"
                value={draft.greeting}
                disabled={!editable}
                onChange={(event) => patch({ greeting: event.target.value })}
                className={INPUT}
              />
            </Field>

            <div className="flex flex-wrap items-center gap-3 rounded-lg bg-paper px-3.5 py-3">
              <p className="min-w-0 flex-1 text-[12.5px] leading-relaxed text-slate">
                <b className="font-semibold text-ink">{shownFaqs.length}개</b>의 공통 질문이 버튼으로
                보입니다. 문구와 답변은 공통 질문에서 관리합니다.
              </p>
              <LinkButton size="sm" href={ROUTES.faq}>
                공통 질문 열기
              </LinkButton>
            </div>
          </CardBody>
        </Card>

        <Card className="mb-4">
          <CardHeader title="말투" />
          <CardBody>
            <Field
              id="persona"
              label="어떤 태도로 답할까요"
              hint="챗봇의 성격을 정합니다. 짧고 구체적으로 쓸수록 잘 따릅니다."
            >
              <textarea
                id="persona"
                rows={4}
                value={draft.persona}
                disabled={!editable}
                onChange={(event) => patch({ persona: event.target.value })}
                className={`${INPUT} resize-y leading-relaxed`}
              />
            </Field>

            <Field id="fallback" label="모를 때 할 말">
              <input
                id="fallback"
                value={draft.fallbackMessage}
                disabled={!editable}
                onChange={(event) => patch({ fallbackMessage: event.target.value })}
                className={INPUT}
              />
            </Field>

            <Field
              id="forbidden"
              label="말하지 말 것"
              hint="여기 적힌 주제는 안내를 거절하고 상담원 연결을 제안합니다. 쉼표로 구분하세요."
            >
              <input
                id="forbidden"
                value={draft.forbiddenTopics.join(", ")}
                disabled={!editable}
                onChange={(event) =>
                  patch({
                    forbiddenTopics: event.target.value
                      .split(",")
                      .map((item) => item.trim())
                      .filter(Boolean),
                  })
                }
                className={INPUT}
              />
            </Field>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="답을 못 찾았을 때" />
          <CardBody className="flex flex-col gap-3">
            <Toggle
              checked={draft.leadCaptureEnabled}
              disabled={!editable}
              onChange={(value) => patch({ leadCaptureEnabled: value })}
              label="연락처 남기기 제안"
              hint="이름과 전화번호를 받아 '남긴 연락처'에 모읍니다"
            />
            <div>
              <Toggle
                checked={draft.supportPhone !== null && draft.supportPhone !== ""}
                disabled={!editable}
                onChange={(value) => patch({ supportPhone: value ? "1588-0000" : null })}
                label="고객센터 번호 안내"
                hint="답을 못 찾으면 이 번호를 알려줍니다"
              />
              {draft.supportPhone ? (
                <input
                  aria-label="고객센터 번호"
                  value={draft.supportPhone}
                  disabled={!editable}
                  onChange={(event) => patch({ supportPhone: event.target.value })}
                  className="mt-2 ml-6 w-[200px] rounded-[7px] border border-line px-2.5 py-1.5 font-mono text-[12.5px]"
                />
              ) : null}
            </div>
            <Toggle
              checked={draft.agentHandoffEnabled}
              disabled={!editable}
              onChange={(value) => patch({ agentHandoffEnabled: value })}
              label="운영시간에는 상담원 연결"
              hint={draft.agentHours ?? "평일 09:00–18:00"}
            />
          </CardBody>
        </Card>
      </div>

      <div className="xl:sticky xl:top-6 xl:self-start">
        <Eyebrow className="mb-2 block">미리보기 · 실제로 이렇게 보입니다</Eyebrow>
        <WidgetPreview
          botName={draft.botName}
          greeting={draft.greeting}
          brandColor={isHexColor(draft.brandColor) ? draft.brandColor : "#17222E"}
          faqs={shownFaqs.map((faq) => ({
            id: faq.id,
            question: faq.question,
            answer: faq.answer,
            links: faq.links,
          }))}
        />

        {error ? (
          <Notice tone="error" className="mt-3">
            {error}
          </Notice>
        ) : null}
        {save.isSuccess && !error ? (
          <Notice tone="info" className="mt-3">
            저장했습니다. 방문자 화면에 바로 반영됩니다.
          </Notice>
        ) : null}

        {editable ? (
          <div className="mt-3 flex gap-2">
            <Button className="flex-1" disabled={localDraft === null} onClick={() => setLocalDraft(null)}>
              되돌리기
            </Button>
            <Button variant="accent" className="flex-1" disabled={save.isPending} onClick={submit}>
              {save.isPending ? "저장 중…" : "저장"}
            </Button>
          </div>
        ) : (
          <Notice tone="info" className="mt-3">
            보기 전용 권한입니다. 설정 변경은 편집 권한이 필요합니다.
          </Notice>
        )}
      </div>
    </div>
  );
}

const INPUT =
  "w-full rounded-[7px] border border-line bg-card px-[11px] py-[8.5px] text-[13.5px] focus:border-ink-3 focus:outline-none disabled:bg-paper";

function isHexColor(value: string): boolean {
  return /^#[0-9a-fA-F]{6}$/.test(value);
}

function Field({
  id,
  label,
  hint,
  children,
}: {
  id: string;
  label: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mb-4">
      <label htmlFor={id} className="mb-1.5 block text-[12.5px] font-medium">
        {label}
      </label>
      {children}
      {hint ? <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">{hint}</p> : null}
    </div>
  );
}

function Toggle({
  checked,
  disabled,
  onChange,
  label,
  hint,
}: {
  checked: boolean;
  disabled: boolean;
  onChange: (value: boolean) => void;
  label: string;
  hint: string;
}) {
  return (
    <label className="flex items-start gap-2.5 text-[13.5px]">
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
        className="mt-1 size-3.5"
      />
      <span>
        {label}
        <span className="mt-0.5 block text-[11.5px] text-slate-2">{hint}</span>
      </span>
    </label>
  );
}
