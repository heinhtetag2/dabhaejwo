import { Fragment } from "preact";
import { useEffect, useRef, useState } from "preact/hooks";

import { WidgetApi, WidgetApiError } from "../api/client";
import type { AskResponse, Faq, Message, WidgetConfig } from "../types";

/**
 * 위젯 본체. Shadow Root 안에서만 산다 — document 를 직접 만지지 않는다.
 * DOM 부착은 loader.ts 의 책임이다.
 */
export function WidgetApp({ config, path }: { config: WidgetConfig; path: string }) {
  /**
   * 서버가 "띄워도 되나"를 답해줄 때까지 아무것도 그리지 않는다.
   *
   * null 은 <b>아직 모른다</b>는 뜻이다. 이걸 false 와 뭉뚱그리면 판정이 오기 전 한 순간
   * 버블이 떴다가 사라져 깜빡임으로 보이고, true 로 두면 업체가 꺼둔 사이트에도 잠깐 뜬다.
   */
  const [visible, setVisible] = useState<boolean | null>(null);
  const [nudgeDelayMs, setNudgeDelayMs] = useState(config.nudgeDelayMs);
  /**
   * 업체가 대시보드에서 정한 위치. 호스트가 지정하지 않았을 때만 쓴다.
   *
   * 판정이 오기 전에는 {@code null} 이지만, 그동안은 {@code visible} 이 아직 null 이라
   * 아무것도 그리지 않으므로 위치가 바뀌며 튀는 일은 없다.
   */
  const [remotePosition, setRemotePosition] = useState<"left" | "right" | null>(null);
  /** 업체가 올린 로고·아이콘. 없으면 기본 말풍선을 그린다. */
  const [launcherImageUrl, setLauncherImageUrl] = useState<string | null>(null);
  const [launcherSizePx, setLauncherSizePx] = useState<number | null>(null);
  /**
   * 이미지 뒤에 깔 것. 서버가 이미 결론을 내려준 값이라 위젯은 그리기만 한다 —
   * 여기서 다시 판단하면 대시보드 미리보기와 규칙이 갈린다.
   */
  const [launcherBg, setLauncherBg] = useState<"BRAND" | "WHITE" | "NONE">("BRAND");
  /**
   * 업체가 정한 버블 색. 스타일에 그대로 들어가는 값이라 <b>형식이 맞을 때만</b> 쓴다 —
   * 서버가 저장할 때 막고 있지만, 여기서도 보는 이유는 이 값이 남의 사이트의
   * style 속성으로 들어가기 때문이다. 한 겹으로 막는 걸 믿지 않는다.
   */
  const [brandColor, setBrandColor] = useState<string | null>(null);

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
  /**
   * 이미 눌러본 공통 질문. 목록을 다시 열었을 때 흐리게 표시한다 —
   * 지우지는 않는다. 방문자가 답을 다시 읽고 싶을 수 있고, 목록에서 항목이
   * 사라지면 "내가 잘못 봤나" 싶어진다.
   */
  const [asked, setAsked] = useState<string[]>([]);
  /** `다른 질문 보기` 로 연 전체 목록. 질문을 하나 던지면 닫는다. */
  const [listOpen, setListOpen] = useState(false);

  const api = useRef(new WidgetApi(config)).current;
  const bodyRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let cancelled = false;
    void api
      .fetchConfig(path)
      .then((remote) => {
        if (cancelled) return;
        setVisible(remote.enabled);
        // 말 거는 시점은 서버가 이긴다 — 업체가 콘솔에서 바꾸면 남의 사이트 코드를
        // 고치지 않고도 반영돼야 한다.
        setNudgeDelayMs(remote.nudgeDelayMs);
        // 위치는 반대다. 호스트가 적었으면 그쪽을 존중한다(아래 position 참조).
        setRemotePosition(remote.widgetPosition === "BOTTOM_LEFT" ? "left" : "right");
        setLauncherImageUrl(remote.launcherImageUrl ?? null);
        setLauncherSizePx(remote.launcherSizePx ?? null);
        setLauncherBg(remote.launcherBackground ?? "BRAND");
        setBrandColor(HEX_COLOR.test(remote.brandColor) ? remote.brandColor : null);
      })
      .catch(() => {
        // 키가 틀렸거나 등록되지 않은 주소다. **조용히 사라진다** —
        // 남의 사이트에 우리 오류를 그리지 않는다.
        if (!cancelled) setVisible(false);
      });
    return () => {
      cancelled = true;
    };
  }, [api, path]);

  useEffect(() => {
    if (!visible || nudgeDelayMs === 0) return;
    const timer = setTimeout(() => setNudging(true), nudgeDelayMs);
    return () => clearTimeout(timer);
  }, [visible, nudgeDelayMs]);

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
  }, [messages, greeting, leadOpen, listOpen, busy]);

  function openPanel() {
    setNudging(false);
    setOpen(true);
    setTimeout(() => inputRef.current?.focus(), 100);
  }

  async function run(question: string, send: (session: string) => Promise<void>) {
    if (!sessionId || busy) return;
    // 물어봤으면 목록은 역할을 다했다. 열어둔 채로 두면 새 답변 아래에 계속 따라다닌다.
    setListOpen(false);
    setMessages((prev) => [...prev, { role: "visitor", text: question }]);
    setBusy(true);
    try {
      await send(sessionId);
    } catch (error) {
      /*
       * **끊긴 세션은 오류가 아니다.** 들고 있던 대화가 이 서비스의 것이 아니게 되면
       * (업체가 설치 스니펫을 다른 서비스 것으로 바꿔 붙이면 그렇게 된다) 서버가
       * `CONVERSATION_NOT_FOUND` 를 준다. 그걸 그대로 보여주면 방문자는 "일시적인 문제"를
       * 읽고 새로고침해야 하는데, 사실은 **한 번 다시 시작하면 그만인 일**이다.
       *
       * 재시도는 딱 한 번이다 — 두 번째도 실패하면 진짜 문제이므로 그때는 말한다.
       */
      if (error instanceof WidgetApiError && error.code === "CONVERSATION_NOT_FOUND") {
        try {
          const session = await api.createSession(path);
          setSessionId(session.sessionId);
          await send(session.sessionId);
          setBusy(false);
          return;
        } catch (retryError) {
          setMessages((prev) => [...prev, botMessage(errorText(retryError))]);
          setBusy(false);
          return;
        }
      }
      setMessages((prev) => [...prev, botMessage(errorText(error))]);
    } finally {
      setBusy(false);
    }
  }

  function receive(result: AskResponse) {
    setMessages((prev) => [
      ...prev,
      {
        role: "bot",
        text: result.answer,
        saved: result.saved,
        links: result.links,
        messageId: result.messageId,
        // 업체가 지정한 후속 질문. 없으면 빈 배열이라 칩 줄에는 `다른 질문 보기` 만 남는다.
        suggestions: result.followUpFaqs,
      },
    ]);
    // 못 답했을 때만 연락처를 제안한다. 업체가 꺼 뒀으면 제안하지 않는다.
    if (!result.answered && leadCapture && !leadDone) {
      setLeadOpen(true);
    }
  }

  async function ask(question: string) {
    await run(question, async (session) => receive(await api.ask(session, question, path)));
  }

  async function askFaq(faq: Faq) {
    setAsked((prev) => (prev.includes(faq.id) ? prev : [...prev, faq.id]));
    await run(faq.question, async (session) => receive(await api.askFaq(session, faq.id)));
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

  /*
   * 업체가 껐거나, 이 페이지가 노출 범위 밖이거나, 아직 판정을 못 받았다.
   *
   * **아무것도 그리지 않는다.** 끄기는 "정상 응답 + 안 보임"이어야 한다 — 오류 말풍선을
   * 남기면 업체 사이트가 고장 난 것처럼 보인다(허용 주소를 지워 막던 우회로가 그랬다).
   */
  if (!visible) {
    return null;
  }

  /*
   * 호스트가 적었으면 그 값, 아니면 업체가 대시보드에서 정한 값.
   *
   * 호스트를 앞에 두는 이유는 <b>호스트만 아는 사정</b>이 있기 때문이다 —
   * 다른 상담 위젯이 이미 오른쪽 아래를 점유했는지는 대시보드가 알 수 없고,
   * 겹치면 두 버블이 서로를 가린다.
   *
   * 서버 응답이 아직 없으면 오른쪽으로 둔다. 여기까지 왔다는 건 판정이 끝났다는 뜻이라
   * 실제로는 도달하지 않지만, 타입을 좁히려면 값이 있어야 한다.
   */
  const position = config.position ?? remotePosition ?? "right";

  /*
   * 색은 CSS 변수 하나만 덮는다. 개별 규칙을 인라인으로 넣으면 스타일시트와 두 벌이 되고,
   * 어느 쪽이 이기는지가 규칙마다 달라진다.
   */
  const rootStyle: Record<string, string> = {};
  if (brandColor) {
    rootStyle["--dw-brand"] = brandColor;
  }
  if (launcherSizePx) {
    rootStyle["--dw-launcher"] = `${launcherSizePx}px`;
  }

  return (
    <div class="root" data-position={position} style={rootStyle}>
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
              <Fragment key={index}>
                <div class={`msg ${message.role}`}>
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

                {/*
                  다음 갈 곳은 **마지막 답변 아래에만** 그린다. 지난 말풍선마다 붙이면
                  스크롤을 올릴 때마다 옛 제안이 다시 나타나 지금 무엇을 하면 되는지가 흐려진다.
                */}
                {message.role === "bot" && index === messages.length - 1 && !busy ? (
                  <div class="sugg">
                    {message.suggestions.map((faq) => (
                      <button key={faq.id} onClick={() => void askFaq(faq)}>
                        {faq.question}
                      </button>
                    ))}
                    {faqs.length > 0 && !listOpen ? (
                      <button class="more" onClick={() => setListOpen(true)}>
                        다른 질문 보기
                      </button>
                    ) : null}
                  </div>
                ) : null}
              </Fragment>
            ))}

            {/*
              `다른 질문 보기` 로 편 전체 목록.

              되돌아가기 버튼을 만들지 않은 이유는, 채팅에서 "뒤로"는 앞선 대화가 사라진다는
              뜻으로 읽히기 때문이다. 방문자는 방금 받은 답을 잃을까 봐 누르지 않는다.
              아래로 쌓이면 그 망설임이 없다.
            */}
            {listOpen ? (
              <div class="sugg list">
                <div class="sugg-title">이런 것들을 물어보실 수 있어요</div>
                {faqs.map((faq) => (
                  <button
                    key={faq.id}
                    class={asked.includes(faq.id) ? "done" : undefined}
                    onClick={() => void askFaq(faq)}
                  >
                    {faq.question}
                  </button>
                ))}
              </div>
            ) : null}

            {/*
              작성 중.

              `role="status"` 라 스크린리더에도 알린다 — 점 세 개는 눈으로만 읽히는 신호다.
              점 자체는 aria-hidden 이다. 하나씩 읽어봐야 뜻이 없다.
            */}
            {busy ? (
              <div class="msg bot typing" role="status" aria-label="답변을 작성하고 있습니다">
                <span aria-hidden="true" />
                <span aria-hidden="true" />
                <span aria-hidden="true" />
              </div>
            ) : null}

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
        <button
          class="bubble"
          data-bg={launcherBg}
          onClick={openPanel}
          aria-label="채팅 열기"
        >
          {launcherImageUrl ? (
            // alt 를 비운다. 옆의 aria-label 이 이미 "채팅 열기"라고 말하므로
            // 로고 이름까지 읽으면 스크린리더가 같은 버튼을 두 번 설명하게 된다.
            // **API 기준으로 절대 경로를 만든다.** 서버는 `/api/public/assets/...` 라는
            // 상대 경로를 준다. 그대로 넣으면 브라우저가 <b>호스트 사이트</b> 기준으로
            // 풀어서 `업체도메인/api/public/...` 을 찾고 404 가 난다 — 우리 도메인이 아니다.
            <img src={new URL(launcherImageUrl, config.apiBaseUrl).toString()} alt="" />
          ) : (
            <LauncherIcon />
          )}
        </button>
      ) : null}
    </div>
  );
}

/**
 * 기본 런처 아이콘.
 *
 * <p>이모지(`💬`)를 쓰지 않는다 — 운영체제가 그리므로 윈도우·맥·안드로이드에서 서로 다른
 * 그림이 나온다. 남의 사이트에 얹히는 요소가 우리가 통제하지 못하는 모양이면 안 된다.
 *
 * <p>선(stroke)으로만 그려 어떤 브랜드 색 위에서도 형태가 남는다. 면으로 채우면
 * 밝은 배경색에서 뭉개진다. `currentColor` 라 색을 따로 관리하지 않는다.
 */
function LauncherIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M20.5 11.7c0 4-3.8 7.2-8.5 7.2-1 0-2-.15-2.9-.42l-4.6 1.5 1.5-3.9C4.6 14.9 3.5 13.4 3.5 11.7c0-4 3.8-7.2 8.5-7.2s8.5 3.2 8.5 7.2Z"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linejoin="round"
      />
    </svg>
  );
}

/** `#RRGGBB` 만 통과시킨다. 서버가 이미 막지만 여기가 남의 사이트에 값을 넣는 지점이다. */
const HEX_COLOR = /^#[0-9a-fA-F]{6}$/;

function botMessage(text: string): Message {
  return { role: "bot", text, saved: false, links: [], messageId: null, suggestions: [] };
}

/**
 * 방문자에게 보일 문구.
 *
 * <p><b>사유마다 다르게 말한다.</b> 전부 한 문구로 뭉치면 방문자는 "기다리면 되는 것"과
 * "기다려도 안 되는 것"을 구분할 수 없고, 업체는 로그를 열기 전까지 원인을 모른다.
 *
 * <p>다만 <b>우리 쪽 사정은 드러내지 않는다</b> — 공급사 이름이나 설정 상태를 남의 사이트
 * 방문자에게 알릴 이유가 없다. 구분은 "다시 오면 되는가"를 기준으로만 한다.
 */
function errorText(error: unknown): string {
  if (!(error instanceof WidgetApiError)) {
    return "지금은 답변을 드리기 어렵습니다.";
  }
  switch (error.code) {
    case "RATE_LIMITED":
      return "질문이 너무 빠릅니다. 잠시 후 다시 물어봐 주세요.";
    case "COST_CAP_REACHED":
    case "QUOTA_EXCEEDED":
      // 오늘·이번 달은 안 된다. 잠시 뒤에 다시 오라고 하면 헛걸음이다.
      return "지금은 상담을 이용할 수 없습니다. 문의는 담당자에게 남겨 주세요.";
    case "LLM_RESPONSE_BLOCKED":
      return "이 질문에는 답변을 드리기 어렵습니다. 다르게 물어봐 주시겠어요?";
    case "TENANT_INACTIVE":
      return "현재 상담을 이용할 수 없습니다.";
    default:
      // 공급사 미설정·호출 실패 등 우리 쪽 문제. 방문자에게는 일시적 오류로만 말한다.
      return "일시적인 문제로 답변하지 못했습니다. 잠시 후 다시 시도해 주세요.";
  }
}
