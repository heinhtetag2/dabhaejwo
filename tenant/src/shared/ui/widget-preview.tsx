"use client";

import { useState } from "react";

import { cn } from "@/shared/lib/cn";

/**
 * 방문자가 보게 될 채팅창 미리보기.
 *
 * <p>공통 질문 화면과 말투·모양 화면이 함께 쓴다. 그래서 feature 가 아니라 shared 에 있다.
 *
 * <p><b>여기서 나가는 답변은 저장 답변뿐이다.</b> 자유 입력을 받아 실제 답을 만들려면
 * 답변 파이프라인이 필요한데 아직 없다. 그래서 입력창은 잠가 두고, 흉내 낸 답을
 * 지어내지 않는다 — 업체가 품질을 오판하게 만드는 것이 빈 화면보다 나쁘다.
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

  const ask = (faq: PreviewFaq) => {
    setTurns((prev) => [
      ...prev,
      { role: "visitor", text: faq.question },
      { role: "bot", text: faq.answer, links: faq.links, saved: true },
    ]);
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
              </Bubble>
            ))
          )}
        </div>

        <footer className="flex items-center gap-2 border-t border-line-2 px-3 py-2.5">
          {/* TODO(stub): 자유 입력 응답은 답변 파이프라인이 붙어야 동작한다. */}
          <input
            disabled
            placeholder="직접 입력은 챗봇 연결 후 사용할 수 있습니다"
            className="min-w-0 flex-1 rounded-full border border-line bg-paper px-3 py-1.5 text-[12px] text-slate-2"
          />
          <span
            aria-hidden
            className="grid size-7 shrink-0 place-items-center rounded-full text-[13px] text-white opacity-50"
            style={{ backgroundColor: brandColor }}
          >
            ↑
          </span>
        </footer>
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
          버튼을 눌러보세요. 저장해 둔 답변이 그대로 나옵니다.
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
