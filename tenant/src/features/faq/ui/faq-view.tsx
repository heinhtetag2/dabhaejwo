"use client";

import { useMemo, useState } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import {
  useCreateFaq,
  useDeleteFaq,
  useFaqListQuery,
  useReorderFaqs,
  useUpdateFaq,
  type Faq,
  type FaqSaveInput,
} from "@/entities/chatbot/faq";
import { Button } from "@/shared/common/button";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { canEdit } from "@/shared/lib/auth-store";
import { WidgetPreview } from "@/shared/ui/widget-preview";

import { FaqEditor } from "./faq-editor";
import { FaqList } from "./faq-list";

/**
 * 공통 질문.
 *
 * <p>여기 등록한 답변은 <b>모델을 거치지 않고 그대로 나간다.</b> 즉시 표시되고
 * 대화 사용량에도 잡히지 않는다. 업체가 이걸 채울수록 원가가 내려간다.
 */
export function FaqView() {
  const { data: context } = useAppContextQuery();
  const { data: faqs, isPending, isError, refetch } = useFaqListQuery();

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [composing, setComposing] = useState(false);

  const create = useCreateFaq();
  const update = useUpdateFaq();
  const remove = useDeleteFaq();
  const reorder = useReorderFaqs();

  const editable = canEdit(context?.member?.role);
  const selected = useMemo(
    () => faqs?.find((faq) => faq.id === selectedId) ?? null,
    [faqs, selectedId],
  );
  const shownFaqs = useMemo(() => (faqs ?? []).filter((faq) => faq.shown), [faqs]);

  if (isPending) {
    return <LoadingState />;
  }
  if (isError) {
    return <ErrorState message="공통 질문을 불러오지 못했습니다" onRetry={() => void refetch()} />;
  }

  const pending = create.isPending || update.isPending || remove.isPending;

  const handleSubmit = (input: FaqSaveInput) => {
    if (selected) {
      update.mutate({ id: selected.id, ...input });
      return;
    }
    create.mutate(input, {
      onSuccess: () => {
        setComposing(false);
        setSelectedId(null);
      },
    });
  };

  const handleMove = (index: number, direction: -1 | 1) => {
    const next = [...faqs];
    const target = index + direction;
    [next[index], next[target]] = [next[target], next[index]];
    reorder.mutate(next.map((faq) => faq.id));
  };

  const handleToggleShown = (faq: Faq) => {
    update.mutate({
      id: faq.id,
      question: faq.question,
      answer: faq.answer,
      links: faq.links,
      followUpFaqIds: faq.followUpFaqIds,
      shown: !faq.shown,
    });
  };

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_336px]">
      <div>
        <Card className="mb-4">
          <CardBody className="text-[13px] leading-relaxed text-slate">
            채팅창을 열면 여기 등록한 질문이 버튼으로 보입니다. 방문자가 버튼을 누르면{" "}
            <b className="font-semibold text-ink">AI가 답을 새로 만들지 않고, 저장해 둔 답변을 그대로</b>{" "}
            보여줍니다. 기다림 없이 바로 뜨고, 대화 사용량에도 들어가지 않습니다.
          </CardBody>
        </Card>

        <Card className="mb-4">
          <CardHeader
            title="공통 질문"
            aside={
              <>
                <Eyebrow>
                  {shownFaqs.length}개 노출 / {faqs.length}개 등록
                </Eyebrow>
                {editable ? (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => {
                      setSelectedId(null);
                      setComposing(true);
                    }}
                  >
                    질문 추가
                  </Button>
                ) : null}
              </>
            }
          />
          {faqs.length === 0 ? (
            <EmptyState
              message="등록된 공통 질문이 없습니다. 자주 오는 질문을 넣어두면 그 답변은 원가 없이 나갑니다."
              action={
                editable ? (
                  <Button size="sm" onClick={() => setComposing(true)}>
                    첫 질문 추가
                  </Button>
                ) : undefined
              }
            />
          ) : (
            <FaqList
              faqs={faqs}
              selectedId={selectedId}
              editable={editable && !reorder.isPending}
              onSelect={(faq) => {
                setComposing(false);
                setSelectedId(faq.id);
              }}
              onMove={handleMove}
              onToggleShown={handleToggleShown}
            />
          )}
        </Card>

        {selected || composing ? (
          <FaqEditor
            faq={selected}
            editable={editable}
            pending={pending}
            onSubmit={handleSubmit}
            onDelete={() => {
              if (!selected) {
                return;
              }
              remove.mutate(selected.id, { onSuccess: () => setSelectedId(null) });
            }}
            onCancel={() => {
              setSelectedId(null);
              setComposing(false);
            }}
          />
        ) : null}
      </div>

      <div className="xl:sticky xl:top-6 xl:self-start">
        <Eyebrow className="mb-2 block">방문자가 채팅창을 열면</Eyebrow>
        <WidgetPreview
          botName={context?.tenant.name ?? "도우미"}
          greeting="안녕하세요! 무엇을 도와드릴까요?"
          brandColor="#17222E"
          faqs={shownFaqs.map((faq) => ({
            id: faq.id,
            question: faq.question,
            answer: faq.answer,
            links: faq.links,
          }))}
        />
      </div>
    </div>
  );
}
