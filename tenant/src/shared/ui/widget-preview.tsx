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
  /** 이 답 다음에 붙일 질문. 위젯과 같은 규칙으로 그린다. */
  followUpFaqIds: string[];
}

/**
 * 후속 질문 최대 개수. 서버·질문 편집 화면과 <b>같은 값이어야 한다</b> —
 * 미리보기가 넷을 보여주는데 실제 위젯이 셋만 그리면 확인할 방법이 없어진다.
 */
const MAX_FOLLOW_UPS = 3;

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
  /** 방금 답한 공통 질문. 이 답 아래에 붙일 후속을 여기서 끌어온다. */
  const [lastFaqId, setLastFaqId] = useState<string | null>(null);
  const [askedIds, setAskedIds] = useState<string[]>([]);
  const [listOpen, setListOpen] = useState(false);
  const preview = usePreviewAnswer();

  const ask = (faq: PreviewFaq) => {
    setLastFaqId(faq.id);
    setAskedIds((prev) => (prev.includes(faq.id) ? prev : [...prev, faq.id]));
    setListOpen(false);
    setTurns((prev) => [
      ...prev,
      { role: "visitor", text: faq.question },
      { role: "bot", text: faq.answer, links: faq.links, saved: true },
    ]);
  };

  /*
   * 서버와 같은 기준으로 고른다 — 지정한 순서대로, 노출 중인 것만, 자기 자신은 빼고.
   * 여기서 규칙이 달라지면 업체는 미리보기를 믿고 설정한 뒤 실제와 다른 화면을 보게 된다.
   */
  const followUps = (() => {
    const source = faqs.find((faq) => faq.id === lastFaqId);
    if (!source) {
      return [];
    }
    return source.followUpFaqIds
      .map((id) => faqs.find((faq) => faq.id === id))
      .filter((faq): faq is PreviewFaq => faq !== undefined && faq.id !== source.id)
      .slice(0, MAX_FOLLOW_UPS);
  })();

  const send = (event: React.FormEvent) => {
    event.preventDefault();
    const question = draft.trim();
    if (!question || preview.isPending) {
      return;
    }
    setDraft("");
    // 자유 질문에는 후속이 없다. 앞 답변의 후속을 그대로 두면 남의 답 밑에 붙은 꼴이 된다.
    setLastFaqId(null);
    setListOpen(false);
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

          {turns.map((turn, index) => (
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
          ))}

          {/*
            작성 중. 실 모델 호출이라 몇 초가 걸린다 — 그동안 아무것도 안 보이면
            업체는 보낸 게 맞나 싶어 다시 누르고, 그만큼 원가가 두 번 나간다.
            <b>미리보기의 원가는 진짜로 나간다.</b>
          */}
          {preview.isPending ? (
            <Bubble role="bot">
              <span
                role="status"
                aria-label="답변을 작성하고 있습니다"
                className="flex items-center gap-1 py-0.5"
              >
                <span aria-hidden className="size-1.5 animate-bounce rounded-full bg-slate-2" />
                <span
                  aria-hidden
                  className="size-1.5 animate-bounce rounded-full bg-slate-2 [animation-delay:150ms]"
                />
                <span
                  aria-hidden
                  className="size-1.5 animate-bounce rounded-full bg-slate-2 [animation-delay:300ms]"
                />
              </span>
            </Bubble>
          ) : null}

          {/*
            제안은 <b>대화 아래에 계속 남는다.</b> 예전에는 첫 질문과 함께 사라져 방문자가
            다른 질문으로 갈 길이 없었다 — 두 번째부터는 직접 타이핑해야 했고, 그러면 저장
            답변 대신 모델을 타서 원가도 올라간다.
          */}
          {faqs.length === 0 ? (
            turns.length === 0 ? (
              <p className="pt-1 text-[11.5px] text-slate-2">
                노출 중인 공통 질문이 없어 버튼이 보이지 않습니다.
              </p>
            ) : null
          ) : (
            <div className="flex flex-wrap gap-1.5 pt-1">
              {turns.length === 0 || listOpen ? (
                faqs.map((faq) => (
                  <Chip
                    key={faq.id}
                    faded={askedIds.includes(faq.id)}
                    onClick={() => ask(faq)}
                  >
                    {faq.question}
                  </Chip>
                ))
              ) : (
                <>
                  {followUps.map((faq) => (
                    <Chip key={faq.id} onClick={() => ask(faq)}>
                      {faq.question}
                    </Chip>
                  ))}
                  <Chip dashed onClick={() => setListOpen(true)}>
                    다른 질문 보기
                  </Chip>
                </>
              )}
            </div>
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

/** 제안 칩. 위젯과 같은 모양 — `다른 질문 보기` 만 점선으로 갈라 답변 버튼과 구분한다. */
function Chip({
  children,
  faded,
  dashed,
  onClick,
}: {
  children: React.ReactNode;
  faded?: boolean;
  dashed?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "rounded-full border border-line bg-card px-2.5 py-1.5 text-left text-[12px] transition-colors hover:bg-line-2/60",
        dashed && "border-dashed text-slate-2",
        faded && "text-slate-2",
      )}
    >
      {children}
    </button>
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
