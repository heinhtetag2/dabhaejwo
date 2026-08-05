import type { AskResponse, SessionResponse, WidgetConfig } from "../types";

const KEY_HEADER = "X-Dabhaejwo-Key";

/**
 * 백엔드 호출. 공개 키만 보내고 그 외의 자격증명은 다루지 않는다.
 * 테넌트 식별·레이트 리밋·Origin 검증은 전부 서버가 한다.
 */
export class WidgetApi {
  constructor(private readonly config: WidgetConfig) {}

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
      throw new WidgetApiError(response.status);
    }

    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }
}

export class WidgetApiError extends Error {
  constructor(readonly status: number) {
    super(`widget api ${status}`);
    this.name = "WidgetApiError";
  }

  /** 일일 원가 상한 도달. 챗봇은 안내 메시지만 띄우고 멈춘다. */
  get costCapped(): boolean {
    return this.status === 503;
  }

  get rateLimited(): boolean {
    return this.status === 429;
  }
}
