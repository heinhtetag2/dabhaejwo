import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { readConfig } from "../src/config";
import { mount } from "../src/loader";

/**
 * 호스트 침범 회귀 테스트.
 *
 * Shadow Root 안에서 쿼리해 검증한다 — document 로 찾아진다면 격리가 깨진 것이므로
 * 그 자체가 실패 케이스다 (widget-embed-script.md).
 */
/**
 * 위젯은 뜨기 전에 서버에 "띄워도 되나"를 묻는다. 그 답이 오기 전에는 아무것도 그리지
 * 않으므로, UI 를 보는 테스트는 응답을 흉내 내고 한 틱 기다려야 한다.
 */
function stubConfig(enabled: boolean) {
  globalThis.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => ({ enabled, widgetPosition: "BOTTOM_RIGHT", nudgeDelayMs: 0 }),
  }) as unknown as typeof fetch;
}

/**
 * 마운트 직후의 fetch 프라미스가 처리되고 preact 가 다시 그릴 때까지.
 * fetch → json() → setState → 렌더로 마이크로태스크가 여러 번 도므로 한 틱으로는 모자란다.
 */
const settle = async () => {
  for (let i = 0; i < 5; i += 1) {
    await new Promise((resolve) => setTimeout(resolve, 0));
  }
};

describe("loader", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
    delete (window as unknown as Record<string, unknown>).dabhaejwo;
    stubConfig(true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("설정이 없으면 조용히 포기하고 DOM 을 건드리지 않는다", () => {
    const before = document.body.childElementCount;

    expect(mount()).toBeNull();
    expect(document.body.childElementCount).toBe(before);
  });

  it("키가 공백이면 마운트하지 않는다 — throw 하지도 않는다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "   " };

    expect(() => mount()).not.toThrow();
    expect(mount()).toBeNull();
  });

  it("정상 설정이면 host element 하나만 body 에 붙인다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    const host = mount();

    expect(host).not.toBeNull();
    // 호스트 페이지에 늘어난 직계 자식은 정확히 하나여야 한다
    expect(document.body.childElementCount).toBe(1);
    expect(host!.shadowRoot).not.toBeNull();
  });

  it("UI 는 Shadow Root 안에만 있고 document 로는 찾아지지 않는다", async () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    const host = mount();
    await settle();

    expect(host!.shadowRoot!.querySelector(".root")).not.toBeNull();
    // 격리가 깨졌다면 여기서 잡힌다
    expect(document.querySelector(".root")).toBeNull();
  });

  it("서버가 끄면 아무것도 그리지 않는다 — 오류 말풍선도 남기지 않는다", async () => {
    // 끄기는 "정상 응답 + 안 보임"이어야 한다. 남의 사이트에 우리 오류를 그리면
    // 업체 사이트가 고장 난 것처럼 보인다.
    stubConfig(false);
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    const host = mount();
    await settle();

    expect(host).not.toBeNull();
    expect(host!.shadowRoot!.querySelector(".root")).toBeNull();
    expect(host!.shadowRoot!.querySelector(".bubble")).toBeNull();
  });

  it("설정 조회가 실패해도 조용히 사라진다", async () => {
    // 키가 틀렸거나 등록되지 않은 주소다. 방문자에게 보일 이유가 없다.
    globalThis.fetch = vi.fn().mockRejectedValue(new Error("network")) as unknown as typeof fetch;
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    const host = mount();
    await settle();

    expect(host!.shadowRoot!.querySelector(".bubble")).toBeNull();
  });

  it("전역 스타일시트를 추가하지 않는다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };
    const before = document.head.querySelectorAll("style, link[rel=stylesheet]").length;

    mount();

    expect(document.head.querySelectorAll("style, link[rel=stylesheet]").length).toBe(before);
  });

  it("두 번 호출해도 하나만 붙는다 — 태그 매니저·SPA 라우팅 대비", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    const first = mount();
    const second = mount();

    expect(document.body.childElementCount).toBe(1);
    expect(second).toBe(first);
  });
});

describe("버블 색", () => {
  it("서버가 준 색이 CSS 변수로 들어간다", async () => {
    // /config 응답에 색이 없으면 대시보드에서 아무리 바꿔도 반영되지 않는다.
    const remote = { enabled: true, widgetPosition: "BOTTOM_RIGHT", brandColor: "#1b6b5c", nudgeDelayMs: 0 };
    expect(/^#[0-9a-fA-F]{6}$/.test(remote.brandColor)).toBe(true);
  });

  it("형식이 틀린 값은 쓰지 않는다 — 남의 사이트 style 속성에 들어가는 값이다", () => {
    const HEX = /^#[0-9a-fA-F]{6}$/;
    expect(HEX.test("red; background: url(evil)")).toBe(false);
    expect(HEX.test("#12345")).toBe(false);
    expect(HEX.test("#1b6b5c")).toBe(true);
  });
});

describe("readConfig", () => {
  beforeEach(() => {
    delete (window as unknown as Record<string, unknown>).dabhaejwo;
  });

  it("기본값을 채운다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    expect(readConfig()).toMatchObject({
      key: "pk_live_test",
      debug: false,
    });
  });

  it("position 을 안 적으면 undefined 다 — 업체가 대시보드에서 정한 위치를 써야 한다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    // 여기서 "right" 로 채워버리면 대시보드 설정이 영영 반영되지 않는다.
    expect(readConfig()!.position).toBeUndefined();
  });

  it("알 수 없는 position 도 미지정으로 다룬다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_test",
      position: "middle",
    };

    expect(readConfig()!.position).toBeUndefined();
  });

  it("호스트가 적은 값은 그대로 살린다 — 다른 위젯과 겹칠 때 피할 수단이다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_test",
      position: "left",
    };

    expect(readConfig()!.position).toBe("left");
  });

  it("nudgeDelayMs 0 은 존중한다 — 자동 인사말을 끄는 설정이다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_test",
      nudgeDelayMs: 0,
    };

    expect(readConfig()!.nudgeDelayMs).toBe(0);
  });
});

/**
 * 답변 뒤에 다음 갈 곳.
 *
 * <p>원래는 첫 질문을 던지는 순간 공통 질문 목록이 사라지고 <b>돌아갈 길이 없었다.</b>
 * 방문자는 대개 궁금한 게 하나가 아닌데, 두 번째부터는 직접 타이핑해야 했다 —
 * 그러면 저장 답변 대신 모델을 타서 우리 원가도 올라간다.
 */
describe("다음 갈 곳", () => {
  const FAQS = [
    { id: "f1", question: "유지보수도 해주시나요?" },
    { id: "f2", question: "비용은 얼마인가요?" },
    { id: "f3", question: "기간은 얼마나 걸리나요?" },
  ];

  /** 후속 질문은 <b>업체가 지정한 것만</b> 온다. 서버가 고른 결론을 그대로 흉내 낸다. */
  function stubChat(followUpFaqs: { id: string; question: string }[]) {
    globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      const body = url.includes("/api/widget/config")
        ? { enabled: true, widgetPosition: "BOTTOM_RIGHT", nudgeDelayMs: 0 }
        : url.includes("/api/widget/session")
          ? {
              sessionId: "s1",
              botName: "상담봇",
              greeting: "안녕하세요",
              brandColor: "#17222E",
              widgetPosition: "BOTTOM_RIGHT",
              leadCaptureEnabled: false,
              faqs: FAQS,
            }
          : {
              answered: true,
              saved: true,
              answer: "1년 무상입니다.",
              links: [],
              messageId: "m1",
              followUpFaqs,
            };
      return { ok: true, status: 200, json: async () => body };
    }) as unknown as typeof fetch;
  }

  beforeEach(() => {
    document.body.innerHTML = "";
    delete (window as unknown as Record<string, unknown>).dabhaejwo;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /** 패널을 열고 첫 공통 질문을 눌러 답변까지 받아둔다. */
  async function askFirstFaq(followUpFaqs: { id: string; question: string }[] = []) {
    stubChat(followUpFaqs);
    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_x",
      apiBaseUrl: "https://api.example.com",
    };
    const host = mount()!;
    await settle();
    const root = host.shadowRoot!;
    root.querySelector<HTMLButtonElement>(".bubble")!.click();
    await settle();
    root.querySelector<HTMLButtonElement>(".sugg button")!.click();
    await settle();
    return root;
  }

  const chipTexts = (root: ShadowRoot) =>
    [...root.querySelectorAll(".sugg button")].map((node) => node.textContent);

  it("답변을 받은 뒤에도 다른 질문으로 갈 길이 남는다", async () => {
    const root = await askFirstFaq();
    // 여기가 원래 막혀 있던 곳이다 — 첫 답변 뒤에는 어떤 버튼도 남지 않았다.
    expect(chipTexts(root)).toContain("다른 질문 보기");
  });

  it("업체가 지정한 후속 질문이 답변 아래에 붙는다", async () => {
    const root = await askFirstFaq([FAQS[1]!]);
    expect(chipTexts(root)).toEqual(["비용은 얼마인가요?", "다른 질문 보기"]);
  });

  it("`다른 질문 보기` 는 대화를 지우지 않고 목록을 아래에 편다", async () => {
    const root = await askFirstFaq();
    const before = root.querySelectorAll(".msg").length;

    root.querySelector<HTMLButtonElement>(".sugg .more")!.click();
    await settle();

    // 앞선 대화가 그대로 남아야 한다. 되돌아가기가 아니라 쌓이는 것이다.
    expect(root.querySelectorAll(".msg").length).toBe(before);
    expect([...root.querySelectorAll(".sugg.list button")].map((n) => n.textContent))
      .toEqual(FAQS.map((faq) => faq.question));
  });

  it("이미 물어본 질문은 목록에서 흐리게 남는다 — 지우지 않는다", async () => {
    const root = await askFirstFaq();
    root.querySelector<HTMLButtonElement>(".sugg .more")!.click();
    await settle();

    const done = [...root.querySelectorAll(".sugg.list button.done")].map((n) => n.textContent);
    expect(done).toEqual(["유지보수도 해주시나요?"]);
  });

  it("목록에서 질문을 고르면 목록은 닫힌다 — 새 답변 아래로 따라다니지 않는다", async () => {
    const root = await askFirstFaq();
    root.querySelector<HTMLButtonElement>(".sugg .more")!.click();
    await settle();

    [...root.querySelectorAll<HTMLButtonElement>(".sugg.list button")]
      .find((node) => node.textContent === "비용은 얼마인가요?")!
      .click();
    await settle();

    expect(root.querySelector(".sugg.list")).toBeNull();
  });
});

/**
 * 작성 중 표시.
 *
 * <p>실 모델 호출은 몇 초가 걸린다. 그동안 아무것도 안 보이면 방문자는 보낸 게 맞나 싶어
 * 같은 질문을 다시 던지고, 그만큼 <b>원가가 두 번 나간다.</b>
 */
describe("작성 중", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
    delete (window as unknown as Record<string, unknown>).dabhaejwo;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("답을 기다리는 동안 점 세 개가 뜨고, 답이 오면 사라진다", async () => {
    let release!: () => void;
    const held = new Promise<void>((resolve) => {
      release = resolve;
    });

    globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/api/widget/config")) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ enabled: true, widgetPosition: "BOTTOM_RIGHT", nudgeDelayMs: 0 }),
        };
      }
      if (url.includes("/api/widget/session")) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            sessionId: "s1",
            botName: "상담봇",
            greeting: "안녕하세요",
            brandColor: "#17222E",
            widgetPosition: "BOTTOM_RIGHT",
            leadCaptureEnabled: false,
            faqs: [],
          }),
        };
      }
      // 답변만 붙잡아 둔다 — 진짜 모델 호출이 느린 상황을 그대로 만든다.
      await held;
      return {
        ok: true,
        status: 200,
        json: async () => ({
          answered: true,
          saved: false,
          answer: "그건 이렇습니다.",
          links: [],
          messageId: "m1",
          followUpFaqs: [],
        }),
      };
    }) as unknown as typeof fetch;

    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_x",
      apiBaseUrl: "https://api.example.com",
    };
    const root = mount()!.shadowRoot!;
    await settle();
    root.querySelector<HTMLButtonElement>(".bubble")!.click();
    await settle();

    const input = root.querySelector<HTMLInputElement>(".foot input")!;
    input.value = "언제 오나요?";
    input.dispatchEvent(new Event("input", { bubbles: true }));
    await settle();
    root.querySelector<HTMLFormElement>(".foot")!.dispatchEvent(
      new Event("submit", { bubbles: true, cancelable: true }),
    );
    await settle();

    const typing = root.querySelector(".typing");
    expect(typing).not.toBeNull();
    // 눈으로만 읽히는 신호라 스크린리더에도 말해줘야 한다.
    expect(typing!.getAttribute("role")).toBe("status");
    expect(typing!.querySelectorAll("span").length).toBe(3);

    release();
    await settle();

    expect(root.querySelector(".typing")).toBeNull();
    expect(root.textContent).toContain("그건 이렇습니다.");
  });
});

/**
 * 끊긴 세션.
 *
 * <p>업체가 설치 스니펫을 <b>다른 서비스의 것으로 바꿔 붙이면</b> 열려 있던 페이지가 들고
 * 있는 대화 id 는 더 이상 그 서비스의 것이 아니다. 서버는 `CONVERSATION_NOT_FOUND` 로
 * 거절하는데, 그걸 그대로 방문자에게 보여주면 <b>"일시적인 문제"</b>가 뜬다 —
 * 사실은 다시 시작하면 그만인 일이고, 방문자는 그 사정을 알 길이 없다.
 */
describe("끊긴 세션", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
    delete (window as unknown as Record<string, unknown>).dabhaejwo;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /** 첫 ask 만 `CONVERSATION_NOT_FOUND` 로 거절하고, 두 번째부터는 정상으로 답한다. */
  function stubStaleSession() {
    let asked = 0;
    let sessions = 0;
    globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/api/widget/config")) {
        return { ok: true, status: 200, json: async () => ({ enabled: true, widgetPosition: "BOTTOM_RIGHT", nudgeDelayMs: 0 }) };
      }
      if (url.includes("/api/widget/session")) {
        sessions += 1;
        return {
          ok: true,
          status: 200,
          json: async () => ({
            sessionId: `s${sessions}`,
            botName: "상담봇",
            greeting: "안녕하세요",
            brandColor: "#17222E",
            widgetPosition: "BOTTOM_RIGHT",
            leadCaptureEnabled: false,
            faqs: [{ id: "f1", question: "유지보수도 해주시나요?" }],
          }),
        };
      }
      asked += 1;
      if (asked === 1) {
        return {
          ok: false,
          status: 404,
          json: async () => ({ code: "CONVERSATION_NOT_FOUND", message: "대화를 찾을 수 없습니다" }),
        };
      }
      return {
        ok: true,
        status: 200,
        json: async () => ({
          answered: true,
          saved: true,
          answer: "1년 무상입니다.",
          links: [],
          messageId: "m1",
          followUpFaqs: [],
        }),
      };
    }) as unknown as typeof fetch;
    return () => ({ asked, sessions });
  }

  it("세션이 끊기면 조용히 새로 시작해 답을 받아낸다 — 오류를 보여주지 않는다", async () => {
    const counts = stubStaleSession();
    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_x",
      apiBaseUrl: "https://api.example.com",
    };
    const host = mount()!;
    await settle();
    const root = host.shadowRoot!;
    root.querySelector<HTMLButtonElement>(".bubble")!.click();
    await settle();
    root.querySelector<HTMLButtonElement>(".sugg button")!.click();
    await settle();

    const text = root.textContent ?? "";
    expect(text).toContain("1년 무상입니다.");
    expect(text).not.toContain("일시적인 문제");
    // 세션을 다시 발급받았고, 질문은 두 번 나갔다(첫 번은 거절당했다).
    expect(counts().sessions).toBe(2);
    expect(counts().asked).toBe(2);
  });
});
