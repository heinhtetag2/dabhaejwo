/** 모델 단가. 키는 api-contracts.md §7 과 일치한다. */

import type { ProviderName } from "@/entities/usage";

export type PurposeKind = "GENERATE" | "EMBED";

/**
 * 단가 한 행.
 *
 * **수정·삭제가 없다.** 단가는 이력이며 행을 추가만 한다 — 기존 행을 고치면
 * 그 단가로 이미 계산된 과거 원가의 근거가 사라진다.
 */
export interface ModelPrice {
  id: number;
  provider: ProviderName;
  model: string;
  purposeKind: PurposeKind;
  inputPer1m: number;
  /** 임베딩 모델은 출력 토큰이 없다. */
  outputPer1m: number | null;
  effectiveFrom: string;
  note: string | null;
  /** 그 (공급사, 모델) 조합에서 지금 적용 중인 행인지. 서버가 판단한다. */
  current: boolean;
}

export interface ModelPriceCreateBody {
  provider: ProviderName;
  model: string;
  purposeKind: PurposeKind;
  inputPer1m: number;
  outputPer1m: number | null;
  /** 비우면 지금부터. 미래를 넣으면 예약이다. */
  effectiveFrom: string | null;
  note: string | null;
  reason: string;
}
