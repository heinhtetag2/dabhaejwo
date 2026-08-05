import { ApiError } from "@/shared/api/http-client";

/**
 * 사용자에게 보일 오류 문구.
 *
 * 서버는 `code`(기계용)와 `message`(사람용)를 함께 준다. message 를 그대로 쓰되,
 * 없거나 알 수 없는 오류면 일반 문구로 덮는다 — 내부 구조가 화면에 새면 안 된다.
 */
export function errorMessage(error: unknown, fallback = "요청을 처리하지 못했습니다"): string {
  if (error instanceof ApiError) {
    return error.message || fallback;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

/** 아직 붙지 않은 외부 의존성 때문에 거절된 요청인가. 화면이 다르게 안내한다. */
export function isFeatureNotReady(error: unknown): boolean {
  return error instanceof ApiError && error.code === "FEATURE_NOT_READY";
}
