import type { WidgetConfig } from "./types";

/** window 전역은 이 하나만 쓴다. 전역 함수·클래스를 등록하지 않는다. */
const GLOBAL_KEY = "dabhaejwo";

/**
 * 기본 API 주소는 **빌드 시점에 박힌다**(`VITE_API_BASE_URL`).
 *
 * 호스트 사이트는 우리 도메인을 알 필요가 없다 — 설치 코드에 키만 적고 끝나야 한다.
 * 그래서 위젯이 자기가 어디로 물어볼지를 알고 있어야 하고, 그 값은 배포 환경마다 다르다.
 * 코드에 박아두면 스테이징 위젯이 운영 API 를 두드린다.
 *
 * 호스트가 `apiBaseUrl` 을 직접 지정하면 그 값이 이긴다(자체 호스팅·디버깅용).
 */
const DEFAULT_API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:4310";

const DEFAULTS = {
  apiBaseUrl: DEFAULT_API_BASE_URL,
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
