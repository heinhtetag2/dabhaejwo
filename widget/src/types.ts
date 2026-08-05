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
  botName: string;
  greeting: string;
  brandColor: string;
  /** 대시보드 API 와 같은 이름·같은 값. 위젯만 다른 말을 쓰면 계약이 두 벌이 된다. */
  widgetPosition: "BOTTOM_RIGHT" | "BOTTOM_LEFT";
  /** false 면 답변 실패 뒤에도 연락처 폼을 제안하지 않는다. */
  leadCaptureEnabled: boolean;
  faqs: Faq[];
}

export interface AskResponse {
  /** false 면 답변 실패 — 연락처 폼을 제안한다. */
  answered: boolean;
  /** true 면 저장 답변이다. 모델을 거치지 않았고 원가도 발생하지 않았다. */
  saved: boolean;
  answer: string;
  links: string[];
  /** 미리보기 응답에는 없다. 👍👎 를 붙일 대상이 없다는 뜻이다. */
  messageId: string | null;
}

export type Message =
  | { role: "visitor"; text: string }
  | { role: "bot"; text: string; saved: boolean; links: string[]; messageId: string | null };
