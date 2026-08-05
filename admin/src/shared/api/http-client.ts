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
  const response = await send(path, options);

  /*
   * 액세스 토큰이 만료됐다(수명 30분). 리프레시 쿠키가 살아 있으면 조용히 재발급받아
   * **원래 요청을 한 번만** 다시 보낸다.
   *
   * 이게 없으면 세션이 새로고침에는 살아남지만 30분마다 화면 한복판에서 끊긴다 —
   * 사용자에겐 "가만히 있었는데 로그아웃됐다"로 보인다.
   *
   * 재발급 자체가 401 이면 다시 시도하지 않는다. 그러면 무한 루프가 된다.
   */
  if (response.status === 401 && path !== REFRESH_PATH) {
    const reissued = await reissueOnce();
    if (reissued) {
      return unwrap<T>(await send(path, options));
    }
    // 되살릴 세션이 없다. 메모리를 비우면 셸 가드가 로그인으로 보낸다.
    useAuthStore.getState().signOut();
  }

  return unwrap<T>(response);
}

const REFRESH_PATH = "/api/auth/refresh";

/**
 * 어느 리프레시 쿠키를 쓸지.
 *
 * 두 콘솔이 같은 API 호스트를 보므로 쿠키 이름이 하나면 나중 로그인이 앞의 세션을
 * 덮어쓴다. 주체별로 쿠키를 나눴고, 이 값이 그중 하나를 고른다.
 *
 * **권한을 주장하는 값이 아니다.** 권한은 쿠키 안의 토큰이 정한다 —
 * 없는 쿠키를 달라고 하면 401 이다.
 */
const SCOPE = "OPS";

/**
 * 동시에 여러 요청이 401 을 받아도 재발급은 <b>한 번만</b> 한다.
 *
 * 화면 하나가 대여섯 개를 병렬로 부르는 일이 흔한데, 각자 재발급을 부르면 같은 쿠키로
 * 동시에 여러 번 갱신하게 되고 어느 응답이 마지막인지도 보장되지 않는다.
 */
let inFlight: Promise<boolean> | null = null;

function reissueOnce(): Promise<boolean> {
  inFlight ??= (async () => {
    try {
      const response = await send(REFRESH_PATH, {
        method: "POST",
        query: { scope: SCOPE },
      });
      if (!response.ok) {
        return false;
      }
      const { accessToken } = (await response.json()) as { accessToken: string };
      useAuthStore.getState().setAccessToken(accessToken);
      return true;
    } catch {
      return false;
    } finally {
      // 다음 만료 때 다시 시도할 수 있게 풀어 준다.
      inFlight = null;
    }
  })();
  return inFlight;
}

async function send(path: string, options: RequestOptions): Promise<Response> {
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

  return fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    // 리프레시 토큰이 httpOnly 쿠키로 오간다. 이 옵션이 없으면 브라우저가 쿠키를
    // 싣지 않아 새로고침 후 세션 복원이 조용히 실패한다 — 서버의 CORS
    // allowCredentials 와 **둘 다** 켜져야 동작한다.
    credentials: "include",
  });
}

async function unwrap<T>(response: Response): Promise<T> {
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
