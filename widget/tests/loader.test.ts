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
