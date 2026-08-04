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
     * 편집 권한을 요구한다. VIEWER 는 거부된다.
     *
     * <p>대리 접속 토큰은 서버가 VIEWER 로 낮춰 발급하므로 여기서 자연히 막힌다 —
     * 운영자가 업체 설정을 바꾸는 일이 없다.
     */
    public static AuthPrincipal.TenantUser requireEditor() {
        AuthPrincipal.TenantUser user = tenantUser();
        if (!user.canEdit()) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return user;
    }

    /**
     * 소유자만 허용한다. 요금제·결제·팀원처럼 되돌리기 어렵거나 돈이 걸린 행위에 건다
     * (tenant-plan.md §8).
     */
    public static AuthPrincipal.TenantUser requireOwner() {
        AuthPrincipal.TenantUser user = tenantUser();
        if (user.role() != TenantMemberRole.OWNER) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return user;
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
