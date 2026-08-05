import type { OperatorRole } from "@/shared/lib/auth-store";

/** 운영자. api-contracts.md §1. 비밀번호 해시는 어떤 응답에도 없다. */
export interface Operator {
  id: string;
  name: string;
  email: string;
  role: OperatorRole;
  /** 비활성 계정은 로그인만 막힌다. 과거 기록에는 이름이 그대로 남는다. */
  active: boolean;
  lastSeenAt: string | null;
}

export interface OpsLoginResponse {
  accessToken: string;
  refreshToken: string;
  operator: Operator;
}

/**
 * 역할이 무엇을 할 수 있는가.
 *
 * **서버가 준다.** 프론트에 매트릭스를 복제해 두면 코드가 바뀌어도 화면은 옛 표를 보여주고,
 * 운영자는 실제와 다른 권한을 믿게 된다.
 */
export interface RolePermissions {
  role: OperatorRole;
  label: string;
  permissions: string[];
}

export interface OperatorCreateBody {
  email: string;
  name: string;
  role: OperatorRole;
  password: string;
  reason: string;
}

export interface OperatorUpdateBody {
  name: string;
  role: OperatorRole;
  reason: string;
}
