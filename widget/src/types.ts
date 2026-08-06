/** 위젯 설정. 공개 키만 받는다 — 시크릿을 받는 필드를 두지 않아 오배치를 타입에서 막는다. */
export interface WidgetConfig {
  /** pk_live_... 남의 사이트 소스에 노출돼도 무방하다. 서버가 Origin 을 검증한다. */
  key: string;
  apiBaseUrl: string;
  /**
   * 버블 위치. <b>미지정({@code undefined})이 의미를 갖는다</b> —
   * 그때는 업체가 대시보드에서 정한 값을 쓴다.
   *
   * 호스트가 적으면 그 값이 이긴다. 호스트만 아는 사정이 있기 때문이다 —
   * 다른 상담 위젯이 이미 한쪽 구석을 점유했는지는 대시보드가 알 수 없다.
   */
  position?: "right" | "left";
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
  /**
   * 이 답 다음에 물어볼 만한 질문. <b>업체가 직접 지정한 것만</b> 온다.
   * 자유 질문에는 늘 빈 배열이다 — 무엇을 물었는지에 맞는 후속을 고를 근거가 없다.
   */
  followUpFaqs: Faq[];
}

export type Message =
  | { role: "visitor"; text: string }
  | {
      role: "bot";
      text: string;
      saved: boolean;
      links: string[];
      messageId: string | null;
      /** 이 말풍선 아래에 칩으로 붙일 질문. 마지막 말풍선에서만 그린다. */
      suggestions: Faq[];
    };

/**
 * 서버가 정하는 노출 규칙. 위젯이 뜨기 전에 받는다.
 *
 * `enabled` 는 **결론만** 담는다 — 업체가 껐는지, 이 경로가 노출 범위 밖인지는
 * 알려주지 않는다. 어떤 페이지를 감추고 싶어 하는지가 남의 사이트 소스에 드러나면 안 된다.
 */
export interface RemoteConfig {
  enabled: boolean;
  /** 런처에 넣을 이미지. 서버가 아이콘 > 로고 순으로 이미 골라 준 값이다. 없으면 기본 아이콘. */
  launcherImageUrl: string | null;
  /** 런처 지름(px). 업체는 3단계 중에서 고르고 픽셀은 서버가 정한다. */
  launcherSizePx: number;
  /**
   * 올린 이미지 뒤에 깔 것.
   *
   * PNG 의 투명은 흰색이 아니라 <b>아무것도 안 칠한 것</b>이라 뒤에 있는 게 그대로 올라온다.
   * 이미지가 없으면 서버가 늘 `BRAND` 를 준다 — 기본 아이콘이 흰 선이라 흰 바탕·투명
   * 위에서는 보이지 않는다.
   */
  launcherBackground: "BRAND" | "WHITE" | "NONE";
  widgetPosition: "BOTTOM_RIGHT" | "BOTTOM_LEFT";
  /** `#RRGGBB`. 스타일에 그대로 들어가므로 쓰기 전에 형식을 한 번 더 본다. */
  brandColor: string;
  nudgeDelayMs: number;
}
