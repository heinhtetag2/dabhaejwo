// admin/shared/config/env.ts 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
// (kickoff-prompt.md §3) 복제해 두었다. 한쪽을 고치면 다른 쪽도 같은 커밋에서 맞춘다.

import { z } from "zod";

/** 환경변수는 여기서 한 번 검증하고 export 한다. 컴포넌트에서 process.env 를 직접 읽지 않는다. */
const schema = z.object({
  apiBaseUrl: z.url(),
  widgetSrc: z.url(),
});

export const env = schema.parse({
  // 포트 4310 = api. admin 은 4311, tenant 는 4312 를 쓴다.
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:4310",
  /**
   * 설치 화면이 업체에게 보여주는 스크립트 주소.
   *
   * **여기가 틀리면 업체가 붙인 코드가 아무 일도 하지 않는다.** 화면에는 정상으로 보이고
   * 위젯만 안 뜨므로, 배포마다 반드시 실제 CDN 주소를 넣는다. 기본값은 로컬 데모용이다.
   */
  widgetSrc: process.env.NEXT_PUBLIC_WIDGET_SRC ?? "http://localhost:5173/w.js",
});
