/**
 * 업체 대시보드 세션. 키는 docs/architecture/api-contracts.md §9-0 과 동일하다.
 * 컴포넌트에서 응답 타입을 다시 정의하지 않는다.
 */

export type TenantMemberRole = "OWNER" | "EDITOR" | "VIEWER";
export type InviteState = "PENDING" | "ACCEPTED";
export type TenantStatus = "TRIAL" | "ACTIVE" | "SUSPENDED" | "CHURNED";

export interface Member {
  id: string;
  name: string | null;
  email: string;
  role: TenantMemberRole;
  inviteState: InviteState;
  lastSeenAt: string | null;
}

/** §2 TenantDetail 의 부분집합. 겹치는 필드는 이름·타입이 완전히 같다. */
export interface TenantContext {
  id: string;
  name: string;
  primaryDomain: string;
  publishableKey: string;
  status: TenantStatus;
  plan: { id: string; name: string; monthlyFee: number };
}

export interface Usage {
  convCount: number;
  convLimit: number;
  docCount: number;
  docLimit: number;
}

/**
 * 운영팀이 대리 접속 중일 때만 값이 있다. null 이 아니면 전 화면 상단에 배너를 고정한다 —
 * 운영자가 자기 계정으로 착각한 채 데이터를 바꾸는 사고를 막기 위함이다 (tenant-plan.md §6.2).
 */
export interface ImpersonationContext {
  sessionId: string;
  reason: string;
  expiresAt: string;
}

export interface AppContext {
  member: Member | null;
  tenant: TenantContext;
  usage: Usage;
  impersonation: ImpersonationContext | null;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  member: Member;
}
