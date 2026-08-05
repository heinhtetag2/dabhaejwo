/** 기능 플래그. 키는 api-contracts.md §8 과 일치한다. */

export type FlagScope = "INTERNAL" | "TENANTS" | "PLAN" | "ALL";

export interface FeatureFlag {
  /** 코드가 이 문자열로 기능을 찾는다. 바꿀 수 없다. */
  key: string;
  name: string;
  description: string | null;
  scope: FlagScope;
  targetTenantIds: string[];
  /** 화면이 "노르드하임 가구 외 2곳"을 그릴 수 있게 서버가 이름을 함께 준다. */
  targetTenantNames: string[];
  targetPlanId: string | null;
  targetPlanName: string | null;
  enabled: boolean;
  updatedAt: string;
}

export interface FeatureFlagUpdateBody {
  scope: FlagScope;
  targetTenantIds: string[];
  targetPlanId: string | null;
  enabled: boolean;
}
