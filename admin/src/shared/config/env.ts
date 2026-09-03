import { z } from "zod";

/** 환경변수는 여기서 한 번 검증하고 export 한다. 컴포넌트에서 process.env 를 직접 읽지 않는다. */
const schema = z.object({
  apiBaseUrl: z.url(),
});

export const env = schema.parse({
  // 포트 4310 = api. admin 은 4311, tenant 는 4312 를 쓴다.
  // `??` 는 undefined 만 잡고 빈 문자열은 통과시킨다 — Vercel 콘솔에 변수를 만들어놓고
  // 값을 비워두면 apiBaseUrl 이 "" 로 z.url() 을 통과 못 해 전체 빌드가 죽는다(실제로 겪음).
  // `||` 로 빈 문자열도 미설정과 같이 취급한다.
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:4310",
});
