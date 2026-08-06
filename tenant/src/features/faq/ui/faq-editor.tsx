"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { faqFormSchema, type Faq, type FaqFormValues, type FaqSaveInput } from "@/entities/chatbot/faq";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { controlClass } from "@/shared/common/control";

/**
 * 후속 질문 최대 개수.
 *
 * <p>서버({@code WidgetChatService.MAX_FOLLOW_UPS})와 <b>같은 값이어야 한다.</b>
 * 여기가 더 크면 업체는 다섯을 골랐는데 방문자에게는 셋만 보이고, 무엇이 잘렸는지
 * 화면 어디에도 나오지 않는다.
 */
const MAX_FOLLOW_UPS = 3;

/**
 * 질문 등록·수정 폼.
 *
 * <p>{@code links} 는 화면에서 쉼표로 입력받고 저장할 때 배열로 바꾼다.
 * 문자열로 저장하면 나중에 항목 단위로 다룰 수 없다.
 */
export function FaqEditor({
  faq,
  allFaqs,
  editable,
  pending,
  onSubmit,
  onDelete,
  onCancel,
}: {
  faq: Faq | null;
  /** 후속 질문 후보. 자기 자신은 여기서 제외된다. */
  allFaqs: Faq[];
  editable: boolean;
  pending: boolean;
  onSubmit: (input: FaqSaveInput) => void;
  onDelete: () => void;
  onCancel: () => void;
}) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FaqFormValues>({
    resolver: zodResolver(faqFormSchema),
    defaultValues: toFormValues(faq),
  });

  /*
   * 후속 질문은 react-hook-form 밖에 둔다 — 체크박스 목록이라 등록/해제 순서가 곧
   * 노출 순서인데, 폼 필드로 다루면 그 순서를 잃는다.
   */
  const [followUps, setFollowUps] = useState<string[]>(faq?.followUpFaqIds ?? []);

  /*
   * 다른 항목으로 갈아탈 때만 선택을 갈아끼운다 — 렌더 중에 맞춘다(React 공식 파생 상태 패턴).
   * 효과로 옮기면 한 프레임 동안 <b>이전 질문의 후속이 새 질문에 붙어 보인다.</b>
   *
   * 저장 뒤 재조회로 같은 id 의 객체가 새로 와도 여기서는 건드리지 않는다.
   * 그때 서버 값으로 되돌리면 저장 직후 사용자가 이어서 고른 것이 사라진다.
   */
  const [syncedId, setSyncedId] = useState(faq?.id ?? null);
  if ((faq?.id ?? null) !== syncedId) {
    setSyncedId(faq?.id ?? null);
    setFollowUps(faq?.followUpFaqIds ?? []);
  }

  // 목록에서 다른 항목을 고르면 폼을 갈아끼운다.
  useEffect(() => {
    reset(toFormValues(faq));
  }, [faq, reset]);

  const submit = handleSubmit((values) => {
    onSubmit({
      question: values.question.trim(),
      answer: values.answer.trim(),
      links: parseLinks(values.linksText),
      followUpFaqIds: followUps,
      shown: values.shown,
    });
  });

  /*
   * 자기 자신은 뺀다 — 방금 읽은 답을 다시 물으라는 버튼이 된다.
   * 노출을 끈 질문도 뺀다. 서버가 같은 기준으로 거르므로, 여기서 고르게 두면
   * 저장은 되는데 방문자에게는 안 보이는 상태가 된다.
   */
  const candidates = allFaqs.filter((item) => item.id !== faq?.id && item.shown);
  /** 실제로 방문자에게 도달할 것만. 상한도 화면 표시도 이 기준이다. */
  const effective = followUps.filter((id) => candidates.some((item) => item.id === id));
  const dropped = followUps.length - effective.length;

  const toggleFollowUp = (id: string) => {
    setFollowUps((prev) =>
      prev.includes(id)
        ? prev.filter((item) => item !== id)
        : // 고른 순서가 방문자에게 보이는 순서다. 뒤에 붙인다.
          [...prev, id],
    );
  };

  return (
    <Card>
      <CardHeader
        title={faq ? "질문 수정" : "질문 추가"}
        aside={faq ? <Eyebrow>{faq.hitCount.toLocaleString()}회 사용됨</Eyebrow> : null}
      />
      <CardBody>
        <form onSubmit={submit} noValidate>
          <Field
            id="faq-question"
            label="질문 — 버튼에 보일 문구"
            hint="버튼 한 줄에 들어가야 합니다. 20자 안쪽을 권합니다."
            error={errors.question?.message}
          >
            <input
              id="faq-question"
              disabled={!editable}
              className={controlClass("md")}
              {...register("question")}
            />
          </Field>

          <Field
            id="faq-answer"
            label="답변"
            hint="적어둔 그대로 방문자에게 보입니다. 줄바꿈도 그대로 유지됩니다."
            error={errors.answer?.message}
          >
            <textarea
              id="faq-answer"
              rows={5}
              disabled={!editable}
              className={controlClass("md", "resize-y leading-relaxed")}
              {...register("answer")}
            />
          </Field>

          <Field
            id="faq-links"
            label="함께 보여줄 문서"
            hint="답변 아래에 링크로 붙습니다. 쉼표로 구분하세요."
          >
            <input
              id="faq-links"
              disabled={!editable}
              placeholder="배송 및 반품 안내, 원목 관리 방법"
              className={controlClass("md")}
              {...register("linksText")}
            />
          </Field>

          <div className="mb-[18px]">
            <span className="mb-1.5 block text-[12.5px] font-medium">
              이 답변 다음에 물어볼 만한 질문
            </span>
            {candidates.length === 0 ? (
              <p className="text-[11.5px] leading-relaxed text-slate-2">
                고를 수 있는 질문이 없습니다. 노출 중인 질문이 둘 이상이면 여기서 고를 수 있습니다.
              </p>
            ) : (
              <div className="max-h-44 overflow-y-auto rounded-[7px] border border-line bg-paper p-2.5">
                {candidates.map((item) => {
                  const checked = followUps.includes(item.id);
                  const order = effective.indexOf(item.id);
                  return (
                    <label
                      key={item.id}
                      className="flex items-start gap-2 py-1 text-[13px] leading-snug"
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        // 상한에 닿으면 <b>더 고를 수만</b> 없게 한다. 이미 고른 것은
                        // 풀 수 있어야 다른 걸로 바꿀 수 있다.
                        disabled={!editable || (!checked && effective.length >= MAX_FOLLOW_UPS)}
                        className="mt-0.5 size-3.5 shrink-0"
                        onChange={() => toggleFollowUp(item.id)}
                      />
                      <span className="min-w-0 flex-1">{item.question}</span>
                      {order >= 0 ? (
                        <span className="shrink-0 font-mono text-[11px] tabular-nums text-slate-2">
                          {order + 1}번째
                        </span>
                      ) : null}
                    </label>
                  );
                })}
              </div>
            )}
            <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">
              방문자가 이 답변을 받으면 아래에 버튼으로 붙습니다. 고른 순서대로 최대{" "}
              {MAX_FOLLOW_UPS}개까지 보입니다.
            </p>
            {dropped > 0 ? (
              <p className="mt-1 text-[11.5px] leading-relaxed text-brick">
                고른 질문 {dropped}개는 노출이 꺼져 있거나 삭제되어 방문자에게 보이지 않습니다.
              </p>
            ) : null}
          </div>

          <div className="mb-[18px]">
            <label className="flex items-start gap-2 text-[13.5px]">
              <input
                type="checkbox"
                disabled={!editable}
                className="mt-1 size-3.5"
                {...register("shown")}
              />
              <span>채팅창을 열었을 때 버튼으로 보이기</span>
            </label>
            <p className="mt-1.5 ml-5.5 text-[11.5px] leading-relaxed text-slate-2">
              꺼두면 버튼에는 안 나오지만, 방문자가 비슷한 내용을 직접 입력하면 이 답변이 쓰입니다.
            </p>
          </div>

          {editable ? (
            <div className="flex flex-wrap gap-2">
              <Button type="submit" variant="accent" size="sm" disabled={pending}>
                {pending ? "저장 중…" : "저장"}
              </Button>
              {faq ? (
                <Button type="button" variant="danger" size="sm" disabled={pending} onClick={onDelete}>
                  삭제
                </Button>
              ) : null}
              <Button type="button" size="sm" onClick={onCancel}>
                취소
              </Button>
            </div>
          ) : (
            <p className="text-[12.5px] text-slate-2">
              보기 전용 권한입니다. 수정하려면 소유자에게 편집 권한을 요청하세요.
            </p>
          )}
        </form>
      </CardBody>
    </Card>
  );
}

function Field({
  id,
  label,
  hint,
  error,
  children,
}: {
  id: string;
  label: string;
  hint?: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mb-[18px]">
      <label htmlFor={id} className="mb-1.5 block text-[12.5px] font-medium">
        {label}
      </label>
      {children}
      {error ? (
        <p role="alert" className="mt-1.5 text-[11.5px] text-brick">
          {error}
        </p>
      ) : hint ? (
        <p className="mt-1.5 text-[11.5px] leading-relaxed text-slate-2">{hint}</p>
      ) : null}
    </div>
  );
}

function toFormValues(faq: Faq | null): FaqFormValues {
  return {
    question: faq?.question ?? "",
    answer: faq?.answer ?? "",
    linksText: (faq?.links ?? []).join(", "),
    shown: faq?.shown ?? true,
  };
}

function parseLinks(text: string): string[] {
  return text
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}
