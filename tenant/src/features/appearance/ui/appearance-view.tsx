"use client";

import { useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import { useBotSettingsDraft } from "@/entities/chatbot/bot-settings";
import { useFaqListQuery } from "@/entities/chatbot/faq";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { canEdit } from "@/shared/lib/auth-store";
import { Notice } from "@/shared/ui/notice";
import { WidgetPreview } from "@/shared/ui/widget-preview";
import { controlClass } from "@/shared/common/control";

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
  const { data: faqs } = useFaqListQuery();
  const { query, save, draft, dirty, patch, reset, submit } = useBotSettingsDraft();

  const [error, setError] = useState<string | null>(null);

  const editable = canEdit(context?.member?.role);
  const shownFaqs = (faqs ?? []).filter((faq) => faq.shown);

  if (query.isPending || draft === undefined) {
    return <LoadingState />;
  }
  if (query.isError) {
    return <ErrorState message="설정을 불러오지 못했습니다" onRetry={() => void query.refetch()} />;
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_380px]">
      <div className="min-w-0">
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
                  className={controlClass("sm", "mt-2 ml-6 w-[200px] font-mono")}
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
            <Button className="flex-1" disabled={!dirty} onClick={reset}>
              되돌리기
            </Button>
            <Button
              variant="accent"
              className="flex-1"
              disabled={save.isPending}
              onClick={() => {
                setError(null);
                submit(setError);
              }}
            >
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

const INPUT = controlClass("md");

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
