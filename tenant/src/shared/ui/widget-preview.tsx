"use client";

import { useState } from "react";

import { usePreviewAnswer } from "@/entities/chatbot/preview";
import { ApiError } from "@/shared/api/http-client";
import { cn } from "@/shared/lib/cn";

/**
 * 방문자가 보게 될 채팅창 미리보기.
 *
 * <p>공통 질문 화면과 말투·모양 화면이 함께 쓴다. 그래서 feature 가 아니라 shared 에 있다.
 *
 * <p>자유 입력은 <b>위젯과 같은 파이프라인</b>을 탄다. 흉내 낸 답을 지어내지 않는다 —
 * 업체가 품질을 오판하게 만드는 것이 빈 화면보다 나쁘다.
 *
 * <p>대화로 기록되지 않으므로 방문자 통계와 답변 개선 목록이 오염되지 않는다.
 * 다만 <b>원가는 실제로 나간다</b> — 모델을 진짜로 부르기 때문이다.
 */
export interface PreviewFaq {
  id: string;
  question: string;
  answer: string;
  links: string[];
}

interface Turn {
  role: "visitor" | "bot";
  text: string;
  links?: string[];
  saved?: boolean;
  /** false 면 근거를 못 찾아 안내 문구로 답한 것이다. 업체가 이 상태를 알아야 자료를 보탠다. */
  failed?: boolean;
}

export function WidgetPreview({
  botName,
  greeting,
  brandColor,
  faqs,
  className,
}: {
  botName: string;
  greeting: string;
  brandColor: string;
  faqs: PreviewFaq[];
  className?: string;
}) {
  const [turns, setTurns] = useState<Turn[]>([]);
  const [draft, setDraft] = useState("");
  const preview = usePreviewAnswer();

  const ask = (faq: PreviewFaq) => {
    setTurns((prev) => [
      ...prev,
      { role: "visitor", text: faq.question },
      { role: "bot", text: faq.answer, links: faq.links, saved: true },
    ]);
  };

  const send = (event: React.FormEvent) => {
    event.preventDefault();
    const question = draft.trim();
    if (!question || preview.isPending) {
      return;
    }
    setDraft("");
    setTurns((prev) => [...prev, { role: "visitor", text: question }]);

    preview
      .mutateAsync(question)
      .then((result) =>
        setTurns((prev) => [
          ...prev,
          {
            role: "bot",
            text: result.answer,
            links: result.links,
            saved: result.saved,
            failed: !result.answered,
          },
        ]),
      )
      .catch((cause: unknown) =>
        setTurns((prev) => [
          ...prev,
          {
            role: "bot",
            text: cause instanceof ApiError ? cause.message : "답변을 만들지 못했습니다",
            failed: true,
          },
        ]),
      );
  };

  return (
    <div className={cn("rounded-card border border-line bg-paper p-4.5", className)}>
      <div className="flex h-[430px] flex-col overflow-hidden rounded-[14px] border border-line bg-card shadow-sm">
        <header className="flex items-center gap-2.5 border-b border-line-2 px-3.5 py-3">
          <span
            aria-hidden
            className="grid size-8 shrink-0 place-items-center rounded-full text-[13px] font-semibold text-white"
            style={{ backgroundColor: brandColor }}
          >
            {botName.trim().charAt(0) || "봇"}
          </span>
          <span className="min-w-0">
            <span className="block truncate text-[13px] font-medium">{botName || "이름 없음"}</span>
            <span className="block text-[11px] text-slate-2">보통 몇 초 안에 답해요</span>
          </span>
        </header>

        <div className="flex-1 space-y-2.5 overflow-y-auto px-3.5 py-3.5">
          <Bubble role="bot">{greeting || "인사말이 비어 있습니다"}</Bubble>

          {turns.length === 0 ? (
            <div className="flex flex-wrap gap-1.5 pt-1">
              {faqs.length === 0 ? (
                <p className="text-[11.5px] text-slate-2">
                  노출 중인 공통 질문이 없어 버튼이 보이지 않습니다.
                </p>
              ) : (
                faqs.map((faq) => (
                  <button
                    key={faq.id}
                    type="button"
                    onClick={() => ask(faq)}
                    className="rounded-full border border-line bg-card px-2.5 py-1.5 text-left text-[12px] transition-colors hover:bg-line-2/60"
                  >
                    {faq.question}
                  </button>
                ))
              )}
            </div>
          ) : (
            turns.map((turn, index) => (
              <Bubble key={index} role={turn.role} brandColor={brandColor}>
                {turn.text}
                {turn.links && turn.links.length > 0 ? (
                  <span className="mt-2 flex flex-wrap gap-1.5 border-t border-line-2 pt-2">
                    {turn.links.map((link) => (
                      <span key={link} className="text-[11px] text-slate underline">
                        {link}
                      </span>
                    ))}
                  </span>
                ) : null}
                {turn.saved ? (
                  <span className="mt-1.5 block font-mono text-[10px] tracking-[0.04em] text-slate-2">
                    저장된 답변 · 대화 사용량에 포함되지 않음
                  </span>
                ) : null}
                {turn.failed ? (
                  <span className="mt-1.5 block text-[10.5px] text-brick">
                    근거를 찾지 못해 안내 문구로 답했습니다 · 지식 소스에 자료를 보태보세요
                  </span>
                ) : null}
              </Bubble>
            ))
          )}
        </div>

        <form onSubmit={send} className="flex items-center gap-2 border-t border-line-2 px-3 py-2.5">
          <input
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            disabled={preview.isPending}
            maxLength={500}
            aria-label="질문 입력"
            placeholder={preview.isPending ? "답을 만드는 중…" : "방문자처럼 직접 물어보세요"}
            className="min-w-0 flex-1 rounded-full border border-line bg-paper px-3 py-1.5 text-[12px]"
          />
          <button
            type="submit"
            disabled={preview.isPending || draft.trim().length === 0}
            aria-label="보내기"
            className="grid size-7 shrink-0 place-items-center rounded-full text-[13px] text-white disabled:opacity-50"
            style={{ backgroundColor: brandColor }}
          >
            ↑
          </button>
        </form>
      </div>

      {turns.length > 0 ? (
        <button
          type="button"
          onClick={() => setTurns([])}
          className="mt-3 w-full rounded-[7px] border border-line bg-card py-1.5 text-[12.5px] transition-colors hover:bg-line-2/60"
        >
          처음 화면으로
        </button>
      ) : (
        <p className="mt-3 text-[11.5px] leading-relaxed text-slate-2">
          버튼을 눌러보세요. 저장해 둔 답변이 그대로 나옵니다. 직접 입력하면 학습한 자료에서
          근거를 찾아 답합니다 — 방문자 통계에는 잡히지 않습니다.
        </p>
      )}
    </div>
  );
}

function Bubble({
  role,
  brandColor,
  children,
}: {
  role: "visitor" | "bot";
  brandColor?: string;
  children: React.ReactNode;
}) {
  if (role === "visitor") {
    return (
      <p
        className="ml-auto w-fit max-w-[85%] rounded-[12px] px-3 py-2 text-[12.5px] text-white"
        style={{ backgroundColor: brandColor }}
      >
        {children}
      </p>
    );
  }
  return (
    <p className="w-fit max-w-[90%] rounded-[12px] border border-line-2 bg-paper px-3 py-2 text-[12.5px] whitespace-pre-line">
      {children}
    </p>
  );
}
