/** 위젯 설정. 공개 키만 받는다 — 시크릿을 받는 필드를 두지 않아 오배치를 타입에서 막는다. */
export interface WidgetConfig {
  /** pk_live_... 남의 사이트 소스에 노출돼도 무방하다. 서버가 Origin 을 검증한다. */
  key: string;
  apiBaseUrl: string;
  position: "right" | "left";
  /** 인사말을 자동으로 띄우기까지의 시간(ms). 0이면 띄우지 않는다. */
  nudgeDelayMs: number;
  debug: boolean;
}

export interface Faq {
  id: string;
  question: string;
}

export interface SessionResponse {
  sessionId: string;
  greeting: string;
  faqs: Faq[];
  brandColor: string;
  position: "right" | "left";
}

export interface AskResponse {
  /** false 면 답변 실패 — 연락처 폼을 제안한다. */
  answered: boolean;
  /** true 면 저장 답변이다. 모델을 거치지 않았고 원가도 발생하지 않았다. */
  saved: boolean;
  answer: string;
  links: string[];
  messageId: string;
}

export type Message =
  | { role: "visitor"; text: string }
  | { role: "bot"; text: string; saved: boolean; links: string[]; messageId: string | null };
