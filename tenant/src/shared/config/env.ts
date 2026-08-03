// admin/shared/config/env.ts 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
// (kickoff-prompt.md §3) 복제해 두었다. 한쪽을 고치면 다른 쪽도 같은 커밋에서 맞춘다.

import { z } from "zod";

/** 환경변수는 여기서 한 번 검증하고 export 한다. 컴포넌트에서 process.env 를 직접 읽지 않는다. */
const schema = z.object({
  apiBaseUrl: z.url(),
});

export const env = schema.parse({
  // 포트 4310 = api. admin 은 4311, tenant 는 4312 를 쓴다.
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:4310",
});
