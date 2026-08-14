/** 요금제와 요금제별 모델 배정. 키는 api-contracts.md §7 과 일치한다. */

import type { ProviderName } from "@/entities/usage";

export interface Plan {
  id: string;
  code: string;
  name: string;
  monthlyFee: number;
  /** 협의가(기업). monthlyFee 0 + negotiable 로 "0원 무료"와 구분한다. */
  negotiable: boolean;
  convLimit: number;
  docLimit: number;
  /** 만들 수 있는 서비스 수. 생성 시점에만 검사한다. */
  botLimit: number;
  tenantCount: number;
  /** 삭제 대신 판매 중단만 한다 — 기존 계약 업체가 남아 있다. */
  sellable: boolean;
}

export interface PlanModelAssignment {
  plan: { id: string; name: string };
  provider: ProviderName;
  model: string;
  chunkCount: number;
  /** 서버가 현재 단가로 계산한 추정치. 단가가 없으면 null. */
  estimatedCostPerConvKrw: number | null;
}
