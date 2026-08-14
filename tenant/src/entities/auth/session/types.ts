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
  /** 표시용이다 — 위젯이 실제로 도는 주소는 서비스마다의 허용 목록이 정한다. */
  primaryDomain: string;
  status: TenantStatus;
  plan: { id: string; name: string; monthlyFee: number };
}

export type BotStatus = "ACTIVE" | "PAUSED" | "DELETING";

/**
 * 서비스 — 챗봇 한 벌. 화면 용어는 "서비스", API·코드는 `bot` 이다
 * ({@code docs/plan/service-plan.md} §3).
 */
export interface Bot {
  id: string;
  name: string;
  primaryDomain: string;
  /** 설치 스니펫에 들어간다. <b>서비스마다 다르다.</b> */
  publishableKey: string;
  status: BotStatus;
  /** 서비스를 지목하지 않는 옛 경로의 착지점. 업체당 하나다. */
  defaultBot: boolean;
  /** null 이면 위젯이 아직 한 번도 호출되지 않았다 — 설치가 확인되지 않았다는 뜻이다. */
  lastCalledAt: string | null;
  /** `DELETING` 일 때만 채워진다. */
  deletedAt: string | null;
  /** 이 시각이 지나면 되돌릴 수 없다. 남은 날짜는 화면이 아니라 이 값이 정한다. */
  purgeAfter: string | null;
  createdAt: string;
}

/** 사용량. <b>전부 업체 합산이다</b> — 계약의 단위가 업체다. */
export interface Usage {
  convCount: number;
  convLimit: number;
  docCount: number;
  docLimit: number;
  botCount: number;
  botLimit: number;
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
  /** 이 업체의 서비스 전부. 선택기·리다이렉트 판정·설치 화면이 이걸로 산다. */
  bots: Bot[];
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
