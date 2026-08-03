package com.dabhaejwo.global.security;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 인증 주체 접근점.
 *
 * <p>테넌트 컨텍스트 없이 테넌트 데이터를 조회하지 않기 위한 관문이다 —
 * 타 테넌트 데이터 접근은 P0 이다.
 */
public final class CurrentAuth {

    private CurrentAuth() {
    }

    public static AuthPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof PrincipalAuthentication auth) {
            return auth.getPrincipal();
        }
        throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }

    public static AuthPrincipal.Operator operator() {
        if (principal() instanceof AuthPrincipal.Operator operator) {
            return operator;
        }
        throw new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    public static AuthPrincipal.TenantUser tenantUser() {
        if (principal() instanceof AuthPrincipal.TenantUser user) {
            return user;
        }
        throw new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    public static AuthPrincipal.Visitor visitor() {
        if (principal() instanceof AuthPrincipal.Visitor visitor) {
            return visitor;
        }
        throw new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    /**
     * 대리 접속 중이면 거부한다. 결제 수단 변경·팀원 초대/삭제·계정 해지·문서 삭제에 건다.
     * tenant-plan.md §6.2 의 금지 목록.
     */
    public static void rejectIfImpersonating() {
        if (principal() instanceof AuthPrincipal.TenantUser user && user.impersonating()) {
            throw new BusinessException(ErrorCode.IMPERSONATION_FORBIDDEN_ACTION);
        }
    }
}
