/**
 * 언어 전환 (헤더·푸터·요금제 화면 한정).
 *
 * <p>쿠키에 저장하고 Server Component 가 그 값을 읽어 렌더링한다 — 클라이언트 상태로
 * 두면 헤더·요금제 화면을 전부 클라이언트 컴포넌트로 만들어야 한다(랜딩이 통째로
 * 클라이언트 번들에 들어간다, public-header.tsx 참조). 쿠키는 민감한 값이 아니라
 * httpOnly 로 감출 이유가 없다 — `LanguageSwitch` 가 직접 쓰고 `router.refresh()` 로
 * 서버 렌더를 다시 받는다.
 *
 * <p>범위: 공개 영역 전체(헤더·푸터·랜딩·요금제·가입·로그인·비밀번호 찾기·초대 수락·
 * 약관/개인정보처리방침 자리) — 화면마다 명시적으로 번역을 넣는다(자동/기계 번역 없음).
 * 대시보드(`/app`)는 아직 범위 밖이다.
 */
export type Language = "en" | "ko";

export const LANGUAGE_COOKIE = "language";
export const DEFAULT_LANGUAGE: Language = "ko";

export function parseLanguage(value: string | undefined): Language {
  return value === "ko" || value === "en" ? value : DEFAULT_LANGUAGE;
}
