import type { AskResponse, RemoteConfig, SessionResponse, WidgetConfig } from "../types";

const KEY_HEADER = "X-Dabhaejwo-Key";

/**
 * 백엔드 호출. 공개 키만 보내고 그 외의 자격증명은 다루지 않는다.
 * 테넌트 식별·레이트 리밋·Origin 검증은 전부 서버가 한다.
 */
export class WidgetApi {
  constructor(private readonly config: WidgetConfig) {}

  /**
   * 위젯이 뜨기 전에 묻는다. <b>대화를 만들지 않는다</b> — 페이지를 열기만 한 방문자가
   * 대화로 잡히면 업체 통계가 오염되고 월 한도까지 깎인다.
   */
  fetchConfig(path: string): Promise<RemoteConfig> {
    const url = new URL("/api/widget/config", this.config.apiBaseUrl);
    url.searchParams.set("path", path);
    return this.get<RemoteConfig>(url);
  }

  /**
   * @param path 방문자가 있던 페이지. 업체가 "어느 페이지에서 물었나"를 보고 그 페이지를 고친다.
   *             전체 URL 이 아니라 경로만 보낸다 — 쿼리스트링에 개인정보가 실려 있을 수 있다.
   */
  createSession(path: string): Promise<SessionResponse> {
    return this.post<SessionResponse>("/api/widget/session", { path });
  }

  ask(sessionId: string, question: string, path: string): Promise<AskResponse> {
    return this.post<AskResponse>("/api/widget/ask", { sessionId, question, path });
  }

  askFaq(sessionId: string, faqId: string): Promise<AskResponse> {
    return this.post<AskResponse>(`/api/widget/faq/${encodeURIComponent(faqId)}`, { sessionId });
  }

  sendFeedback(messageId: string, helpful: boolean): Promise<void> {
    return this.post<void>("/api/widget/feedback", { messageId, helpful });
  }

  submitLead(sessionId: string, name: string, contact: string): Promise<void> {
    return this.post<void>("/api/widget/lead", { sessionId, name, contact });
  }

  private async get<T>(url: URL): Promise<T> {
    const response = await fetch(url, {
      headers: { [KEY_HEADER]: this.config.key },
    });
    if (!response.ok) {
      throw await widgetError(response);
    }
    return (await response.json()) as T;
  }

  private async post<T>(path: string, body: unknown): Promise<T> {
    const response = await fetch(new URL(path, this.config.apiBaseUrl), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [KEY_HEADER]: this.config.key,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw await widgetError(response);
    }

    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }
}

/**
 * 서버가 주는 {@code code} 를 함께 담는다.
 *
 * <p>처음엔 status 만 봤는데, <b>503 하나에 원인이 일곱 가지</b>였다(원가 상한·공급사 미설정·
 * 공급사 호출 실패·안전 정책 차단·자격증명 복호화 실패…). 전부 "오늘은 상담이 어렵습니다"로
 * 나가니 방문자는 한도가 찬 줄 알고, 업체는 로그를 열기 전까지 원인을 모른다.
 */
async function widgetError(response: Response): Promise<WidgetApiError> {
  let code = "UNKNOWN";
  try {
    const body = (await response.json()) as { code?: string };
    if (typeof body?.code === "string") {
      code = body.code;
    }
  } catch {
    // 본문이 JSON 이 아니다(프록시가 만든 502 등). status 만으로 판단한다.
  }
  return new WidgetApiError(response.status, code);
}

export class WidgetApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string = "UNKNOWN",
  ) {
    super(`widget api ${status} ${code}`);
    this.name = "WidgetApiError";
  }
}
