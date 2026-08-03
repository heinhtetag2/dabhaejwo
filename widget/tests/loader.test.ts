import { beforeEach, describe, expect, it } from "vitest";

import { readConfig } from "../src/config";
import { mount } from "../src/loader";

/**
 * 호스트 침범 회귀 테스트.
 *
 * Shadow Root 안에서 쿼리해 검증한다 — document 로 찾아진다면 격리가 깨진 것이므로
 * 그 자체가 실패 케이스다 (widget-embed-script.md).
 */
describe("loader", () => {
  beforeEach(() => {
    document.body.innerHTML = "";
    delete (window as unknown as Record<string, unknown>).dabhaejwo;
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

  it("UI 는 Shadow Root 안에만 있고 document 로는 찾아지지 않는다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = { key: "pk_live_test" };

    const host = mount();

    expect(host!.shadowRoot!.querySelector(".root")).not.toBeNull();
    // 격리가 깨졌다면 여기서 잡힌다
    expect(document.querySelector(".root")).toBeNull();
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
      position: "right",
      debug: false,
    });
  });

  it("알 수 없는 position 은 기본값으로 떨어진다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_test",
      position: "middle",
    };

    expect(readConfig()!.position).toBe("right");
  });

  it("nudgeDelayMs 0 은 존중한다 — 자동 인사말을 끄는 설정이다", () => {
    (window as unknown as Record<string, unknown>).dabhaejwo = {
      key: "pk_live_test",
      nudgeDelayMs: 0,
    };

    expect(readConfig()!.nudgeDelayMs).toBe(0);
  });
});
