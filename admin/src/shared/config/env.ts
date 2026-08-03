import { z } from "zod";

/** 환경변수는 여기서 한 번 검증하고 export 한다. 컴포넌트에서 process.env 를 직접 읽지 않는다. */
const schema = z.object({
  apiBaseUrl: z.url(),
});

export const env = schema.parse({
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
});
