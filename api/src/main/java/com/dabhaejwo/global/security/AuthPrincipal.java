package com.dabhaejwo.global.security;

import java.util.UUID;

/**
 * 인증 주체. 신뢰 수준이 전혀 다른 셋을 한 타입으로 뭉뚱그리지 않는다.
 *
 * <p>운영자는 남의 고객 데이터를 볼 수 있고, 업체 담당자는 자기 테넌트만 볼 수 있고,
 * 방문자는 익명이다. 토큰과 필터 체인도 이에 맞춰 분리되어 있다.
 */
public sealed interface AuthPrincipal
        permits AuthPrincipal.Operator, AuthPrincipal.TenantUser, AuthPrincipal.Visitor {

    /** 운영 콘솔. {@code /api/ops/**} */
    record Operator(UUID operatorId, String name, OperatorRole role) implements AuthPrincipal {

        public boolean can(Permission permission) {
            return role.can(permission);
        }
    }

    /**
     * 업체 대시보드. {@code /api/app/**}
     *
     * @param impersonationSessionId 운영자가 대리 접속 중이면 세션 id, 아니면 null.
     *                               null 이 아니면 파괴적 조작이 차단되고 조회는 감사 기록에 남는다.
     */
    record TenantUser(UUID tenantId,
                      UUID memberId,
                      TenantMemberRole role,
                      UUID impersonationSessionId,
                      UUID impersonatingOperatorId) implements AuthPrincipal {

        public boolean impersonating() {
            return impersonationSessionId != null;
        }

        public boolean canEdit() {
            return role == TenantMemberRole.OWNER || role == TenantMemberRole.EDITOR;
        }
    }

    /** 방문자 위젯. {@code /api/widget/**} — 익명이며 공개 키 + Origin 으로 테넌트만 식별한다. */
    record Visitor(UUID tenantId, String origin) implements AuthPrincipal {
    }
}
