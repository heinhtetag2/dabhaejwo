// admin/shared/api/http-client.ts 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
// (kickoff-prompt.md §3) 복제해 두었다. 한쪽을 고치면 다른 쪽도 같은 커밋에서 맞춘다.

import { env } from "@/shared/config/env";
import { currentAccessToken, useAuthStore } from "@/shared/lib/auth-store";

/** 서버가 내려주는 에러 형태 (api-contracts.md §0-3). */
export interface ApiErrorBody {
  code: string;
  message: string;
}

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiError";
    this.code = body.code;
    this.status = status;
  }
}

/** 페이지네이션 응답 (api-contracts.md §0-2). */
export interface PageResponse<T> {
  content: T[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

type QueryValue = string | number | boolean | null | undefined;

interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  query?: Record<string, QueryValue>;
  body?: unknown;
}

/**
 * 모든 API 호출은 이 단일 인스턴스를 지난다. 컴포넌트에서 fetch 를 직접 부르지 않는다.
 */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", query, body } = options;

  const url = new URL(path, env.apiBaseUrl);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== null && value !== undefined && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const headers: Record<string, string> = { Accept: "application/json" };
  const token = currentAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (response.status === 401) {
    // 토큰이 죽었다. 메모리에서 지우고 셸 가드가 로그인으로 보내게 한다.
    useAuthStore.getState().signOut();
  }

  if (!response.ok) {
    const fallback: ApiErrorBody = {
      code: "UNKNOWN",
      message: "요청을 처리하지 못했습니다",
    };
    const parsed = await response.json().catch(() => fallback);
    throw new ApiError(response.status, parsed as ApiErrorBody);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
