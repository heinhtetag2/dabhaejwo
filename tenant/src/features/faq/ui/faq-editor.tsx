"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";

import { faqFormSchema, type Faq, type FaqFormValues, type FaqSaveInput } from "@/entities/chatbot/faq";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { controlClass } from "@/shared/common/control";

/**
 * 질문 등록·수정 폼.
 *
 * <p>{@code links} 는 화면에서 쉼표로 입력받고 저장할 때 배열로 바꾼다.
 * 문자열로 저장하면 나중에 항목 단위로 다룰 수 없다.
 */
export function FaqEditor({
  faq,
  editable,
  pending,
  onSubmit,
  onDelete,
  onCancel,
}: {
  faq: Faq | null;
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

  // 목록에서 다른 항목을 고르면 폼을 갈아끼운다.
  useEffect(() => {
    reset(toFormValues(faq));
  }, [faq, reset]);

  const submit = handleSubmit((values) => {
    onSubmit({
      question: values.question.trim(),
      answer: values.answer.trim(),
      links: parseLinks(values.linksText),
      // 후속 질문은 아직 화면이 없다. 기존 값을 그대로 넘겨 지워지지 않게 한다.
      followUpFaqIds: faq?.followUpFaqIds ?? [],
      shown: values.shown,
    });
  });

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
