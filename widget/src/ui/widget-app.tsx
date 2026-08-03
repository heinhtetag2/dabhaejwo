import { useEffect, useRef, useState } from "preact/hooks";

import { WidgetApi, WidgetApiError } from "../api/client";
import type { Faq, Message, WidgetConfig } from "../types";

/**
 * 위젯 본체. Shadow Root 안에서만 산다 — document 를 직접 만지지 않는다.
 * DOM 부착은 loader.ts 의 책임이다.
 */
export function WidgetApp({ config }: { config: WidgetConfig }) {
  const [open, setOpen] = useState(false);
  const [nudging, setNudging] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [greeting, setGreeting] = useState("");
  const [faqs, setFaqs] = useState<Faq[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);

  const api = useRef(new WidgetApi(config)).current;
  const bodyRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (config.nudgeDelayMs === 0) return;
    const timer = setTimeout(() => setNudging(true), config.nudgeDelayMs);
    return () => clearTimeout(timer);
  }, [config.nudgeDelayMs]);

  useEffect(() => {
    if (!open || sessionId) return;
    let cancelled = false;
    void api
      .createSession()
      .then((session) => {
        if (cancelled) return;
        setSessionId(session.sessionId);
        setGreeting(session.greeting);
        setFaqs(session.faqs);
      })
      .catch(() => {
        if (cancelled) return;
        setMessages([botMessage("지금은 답변을 드리기 어렵습니다. 잠시 후 다시 시도해 주세요.")]);
      });
    return () => {
      cancelled = true;
    };
  }, [open, sessionId, api]);

  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [messages, greeting]);

  function openPanel() {
    setNudging(false);
    setOpen(true);
    setTimeout(() => inputRef.current?.focus(), 100);
  }

  async function run(question: string, send: () => Promise<void>) {
    if (!sessionId || busy) return;
    setMessages((prev) => [...prev, { role: "visitor", text: question }]);
    setBusy(true);
    try {
      await send();
    } catch (error) {
      setMessages((prev) => [...prev, botMessage(errorText(error))]);
    } finally {
      setBusy(false);
    }
  }

  async function ask(question: string) {
    await run(question, async () => {
      const result = await api.ask(sessionId!, question);
      setMessages((prev) => [
        ...prev,
        {
          role: "bot",
          text: result.answer,
          saved: result.saved,
          links: result.links,
          messageId: result.messageId,
        },
      ]);
    });
  }

  async function askFaq(faq: Faq) {
    await run(faq.question, async () => {
      const result = await api.askFaq(sessionId!, faq.id);
      setMessages((prev) => [
        ...prev,
        {
          role: "bot",
          text: result.answer,
          saved: result.saved,
          links: result.links,
          messageId: result.messageId,
        },
      ]);
    });
  }

  function submit(event: Event) {
    event.preventDefault();
    const question = draft.trim();
    if (!question) return;
    setDraft("");
    void ask(question);
  }

  return (
    <div class="root" data-position={config.position}>
      {open ? (
        <div class="panel" role="dialog" aria-label="챗봇 상담">
          <div class="head">
            <span class="name">무엇이든 물어보세요</span>
            <button class="close" onClick={() => setOpen(false)} aria-label="닫기">
              ×
            </button>
          </div>

          <div class="body" ref={bodyRef}>
            {greeting ? <div class="msg bot">{greeting}</div> : null}

            {messages.length === 0 && faqs.length > 0 ? (
              <div class="sugg">
                {faqs.map((faq) => (
                  <button key={faq.id} onClick={() => void askFaq(faq)}>
                    {faq.question}
                  </button>
                ))}
              </div>
            ) : null}

            {messages.map((message, index) => (
              <div key={index} class={`msg ${message.role}`}>
                {/* 저장 답변은 모델을 거치지 않은 것이다. 방문자에게도 즉답임을 알린다 */}
                {message.role === "bot" && message.saved ? (
                  <div class="saved-tag">저장된 답변</div>
                ) : null}
                {message.text}
              </div>
            ))}
          </div>

          <form class="foot" onSubmit={submit}>
            <input
              ref={inputRef}
              value={draft}
              onInput={(e) => setDraft((e.target as HTMLInputElement).value)}
              placeholder="궁금한 점을 입력하세요"
              aria-label="질문 입력"
              disabled={busy || !sessionId}
            />
            <button type="submit" disabled={busy || !draft.trim()} aria-label="보내기">
              ↑
            </button>
          </form>
          <div class="brandline">답해줘로 만든 챗봇</div>
        </div>
      ) : null}

      {!open && nudging ? (
        <button class="nudge" onClick={openPanel}>
          안녕하세요! 궁금한 점이 있으면 물어보세요.
        </button>
      ) : null}

      {!open ? (
        <button class="bubble" onClick={openPanel} aria-label="채팅 열기">
          💬
        </button>
      ) : null}
    </div>
  );
}

function botMessage(text: string): Message {
  return { role: "bot", text, saved: false, links: [], messageId: null };
}

function errorText(error: unknown): string {
  if (error instanceof WidgetApiError) {
    if (error.costCapped) {
      return "오늘은 상담이 어렵습니다. 잠시 후 다시 시도해 주세요.";
    }
    if (error.rateLimited) {
      return "질문이 너무 빠릅니다. 잠시 후 다시 시도해 주세요.";
    }
  }
  return "지금은 답변을 드리기 어렵습니다.";
}
