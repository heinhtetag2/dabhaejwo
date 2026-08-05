/** 공급사 연결. 키는 api-contracts.md §7 과 일치한다. */

import type { ProviderName } from "@/entities/usage";

/** 키가 어디서 왔는가. "새 키를 넣었는데 왜 옛 키로 도나"를 화면이 답할 수 있어야 한다. */
export type CredentialSource = "CONSOLE" | "ENV" | "NONE";

/**
 * **API 키 원문을 담는 필드가 없다.** 서버가 내려주지 않는다 —
 * 한 번 넣으면 사람이 다시 볼 수 없고 교체만 할 수 있다.
 */
export interface ProviderCredential {
  provider: ProviderName;
  configured: boolean;
  enabled: boolean;
  /** 마스킹된 힌트(`AIza…1234`) 또는 `환경변수`. 미설정이면 null. */
  keyHint: string | null;
  source: CredentialSource;
  updatedAt: string | null;
  updatedByName: string | null;
}
