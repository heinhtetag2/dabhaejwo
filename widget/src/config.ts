import type { WidgetConfig } from "./types";

/** window 전역은 이 하나만 쓴다. 전역 함수·클래스를 등록하지 않는다. */
const GLOBAL_KEY = "dabhaejwo";

const DEFAULTS = {
  apiBaseUrl: "https://api.dabhaejwo.com",
  position: "right",
  nudgeDelayMs: 15_000,
  debug: false,
} as const;

/**
 * 호스트 페이지가 넣어둔 설정을 읽는다.
 *
 * 설정이 없거나 키가 비어 있으면 null 을 돌려주고 마운트를 포기한다 —
 * 예외를 던지면 호스트 페이지의 다른 스크립트까지 멈출 수 있다.
 */
export function readConfig(scope: Window = window): WidgetConfig | null {
  const raw = (scope as unknown as Record<string, unknown>)[GLOBAL_KEY];
  if (!raw || typeof raw !== "object") {
    return null;
  }

  const input = raw as Partial<WidgetConfig>;
  if (typeof input.key !== "string" || input.key.trim() === "") {
    return null;
  }

  return {
    key: input.key.trim(),
    apiBaseUrl: input.apiBaseUrl ?? DEFAULTS.apiBaseUrl,
    position: input.position === "left" ? "left" : DEFAULTS.position,
    nudgeDelayMs:
      typeof input.nudgeDelayMs === "number" && input.nudgeDelayMs >= 0
        ? input.nudgeDelayMs
        : DEFAULTS.nudgeDelayMs,
    debug: input.debug === true,
  };
}
