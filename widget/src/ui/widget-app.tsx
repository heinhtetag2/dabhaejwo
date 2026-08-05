import { useEffect, useRef, useState } from "preact/hooks";

import { WidgetApi, WidgetApiError } from "../api/client";
import type { Faq, Message, WidgetConfig } from "../types";

/**
 * 위젯 본체. Shadow Root 안에서만 산다 — document 를 직접 만지지 않는다.
 * DOM 부착은 loader.ts 의 책임이다.
 */
export function WidgetApp({ config, path }: { config: WidgetConfig; path: string }) {
  const [open, setOpen] = useState(false);
  const [nudging, setNudging] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [botName, setBotName] = useState("");
  const [greeting, setGreeting] = useState("");
  const [faqs, setFaqs] = useState<Faq[]>([]);
  const [leadCapture, setLeadCapture] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  /** 답변 실패 뒤에만 연다. 처음부터 띄우면 물어보러 온 사람에게 폼을 들이미는 꼴이다. */
  const [leadOpen, setLeadOpen] = useState(false);
  const [leadDone, setLeadDone] = useState(false);
  const [voted, setVoted] = useState<Record<string, boolean>>({});

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
      .createSession(path)
      .then((session) => {
        if (cancelled) return;
        setSessionId(session.sessionId);
        setBotName(session.botName);
        setGreeting(session.greeting);
        setFaqs(session.faqs);
        setLeadCapture(session.leadCaptureEnabled);
      })
      .catch(() => {
        if (cancelled) return;
        setMessages([botMessage("지금은 답변을 드리기 어렵습니다. 잠시 후 다시 시도해 주세요.")]);
      });
    return () => {
      cancelled = true;
    };
  }, [open, sessionId, api, path]);

  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [messages, greeting, leadOpen]);

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

  function receive(result: {
    answer: string;
    saved: boolean;
    links: string[];
    messageId: string | null;
    answered: boolean;
  }) {
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
    // 못 답했을 때만 연락처를 제안한다. 업체가 꺼 뒀으면 제안하지 않는다.
    if (!result.answered && leadCapture && !leadDone) {
      setLeadOpen(true);
    }
  }

  async function ask(question: string) {
    await run(question, async () => receive(await api.ask(sessionId!, question, path)));
  }

  async function askFaq(faq: Faq) {
    await run(faq.question, async () => receive(await api.askFaq(sessionId!, faq.id)));
  }

  function submit(event: Event) {
    event.preventDefault();
    const question = draft.trim();
    if (!question) return;
    setDraft("");
    void ask(question);
  }

  /**
   * 👍👎. 실패해도 방문자에게 알리지 않는다 — 평가는 우리 쪽 사정이고,
   * 여기서 오류를 띄우면 방문자는 자기 질문이 잘못된 줄 안다.
   */
  function vote(messageId: string, helpful: boolean) {
    setVoted((prev) => ({ ...prev, [messageId]: helpful }));
    void api.sendFeedback(messageId, helpful).catch(() => undefined);
  }

  function submitLead(event: Event) {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const name = (form.elements.namedItem("name") as HTMLInputElement).value.trim();
    const contact = (form.elements.namedItem("contact") as HTMLInputElement).value.trim();
    if (!name || !contact || !sessionId) return;

    setBusy(true);
    void api
      .submitLead(sessionId, name, contact)
      .then(() => {
        setLeadDone(true);
        setLeadOpen(false);
        setMessages((prev) => [...prev, botMessage("남겨주셔서 감사합니다. 확인 후 연락드리겠습니다.")]);
      })
      .catch(() => {
        setMessages((prev) => [...prev, botMessage("연락처를 남기지 못했습니다. 잠시 후 다시 시도해 주세요.")]);
      })
      .finally(() => setBusy(false));
  }

  return (
    <div class="root" data-position={config.position}>
      {open ? (
        <div class="panel" role="dialog" aria-label="챗봇 상담">
          <div class="head">
            <span class="name">{botName || "무엇이든 물어보세요"}</span>
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

                {message.role === "bot" && message.links.length > 0 ? (
                  <div class="links">
                    {message.links.map((link) => (
                      <a key={link} href={link} target="_blank" rel="noopener noreferrer">
                        {link}
                      </a>
                    ))}
                  </div>
                ) : null}

                {message.role === "bot" && message.messageId ? (
                  <div class="vote">
                    <button
                      onClick={() => vote(message.messageId!, true)}
                      aria-label="도움이 됐어요"
                      aria-pressed={voted[message.messageId] === true}
                      disabled={message.messageId in voted}
                    >
                      👍
                    </button>
                    <button
                      onClick={() => vote(message.messageId!, false)}
                      aria-label="도움이 안 됐어요"
                      aria-pressed={voted[message.messageId] === false}
                      disabled={message.messageId in voted}
                    >
                      👎
                    </button>
                  </div>
                ) : null}
              </div>
            ))}

            {leadOpen ? (
              <form class="lead" onSubmit={submitLead}>
                <div class="lead-title">연락처를 남겨주시면 담당자가 확인 후 연락드립니다.</div>
                <input name="name" placeholder="이름" aria-label="이름" required maxLength={60} />
                <input
                  name="contact"
                  placeholder="연락처 또는 이메일"
                  aria-label="연락처 또는 이메일"
                  required
                  maxLength={120}
                />
                <div class="lead-actions">
                  <button type="submit" disabled={busy}>
                    남기기
                  </button>
                  <button type="button" class="ghost" onClick={() => setLeadOpen(false)}>
                    괜찮아요
                  </button>
                </div>
              </form>
            ) : null}
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
