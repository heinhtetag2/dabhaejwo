package com.dabhaejwo.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 권한 매트릭스는 기획서(admin-console-plan §7 / tenant-plan §8)와 일치해야 한다.
 * 여기서 깨지면 기획서와 코드 중 하나가 틀린 것이므로 같은 커밋에서 맞춘다.
 */
class OperatorRoleTest {

    @Test
    @DisplayName("운영 관리자만 모델 단가와 비용 안전장치를 바꿀 수 있다")
    void onlyAdminTouchesPricingAndGuards() {
        assertTrue(OperatorRole.OPS_ADMIN.can(Permission.MODEL_PRICE_WRITE));
        assertTrue(OperatorRole.OPS_ADMIN.can(Permission.COST_GUARD_WRITE));

        for (OperatorRole role : new OperatorRole[]{OperatorRole.CS, OperatorRole.SALES, OperatorRole.DEV}) {
            assertFalse(role.can(Permission.MODEL_PRICE_WRITE), role + " 는 단가를 바꿀 수 없어야 한다");
            assertFalse(role.can(Permission.COST_GUARD_WRITE), role + " 는 안전장치를 바꿀 수 없어야 한다");
        }
    }

    @Test
    @DisplayName("대리 로그인은 관리자·CS·개발만 가능하고 영업은 불가하다")
    void impersonationIsRestricted() {
        assertTrue(OperatorRole.OPS_ADMIN.can(Permission.TENANT_IMPERSONATE));
        assertTrue(OperatorRole.CS.can(Permission.TENANT_IMPERSONATE));
        // 재현이 안 되는 버그 문의가 개발로 넘어오기 때문에 허용한다
        assertTrue(OperatorRole.DEV.can(Permission.TENANT_IMPERSONATE));
        assertFalse(OperatorRole.SALES.can(Permission.TENANT_IMPERSONATE));
    }

    @Test
    @DisplayName("쿼터 증량은 관리자·CS만 가능하다")
    void quotaGrantIsRestricted() {
        assertTrue(OperatorRole.OPS_ADMIN.can(Permission.QUOTA_GRANT));
        assertTrue(OperatorRole.CS.can(Permission.QUOTA_GRANT));
        assertFalse(OperatorRole.SALES.can(Permission.QUOTA_GRANT));
        assertFalse(OperatorRole.DEV.can(Permission.QUOTA_GRANT));
    }

    @Test
    @DisplayName("일시정지·해지와 감사 기록 열람은 관리자 전용이다")
    void adminOnlyActions() {
        for (OperatorRole role : new OperatorRole[]{OperatorRole.CS, OperatorRole.SALES, OperatorRole.DEV}) {
            assertFalse(role.can(Permission.TENANT_STATUS_WRITE), role + " 는 상태를 바꿀 수 없어야 한다");
            assertFalse(role.can(Permission.AUDIT_READ), role + " 는 감사 기록을 볼 수 없어야 한다");
        }
        assertTrue(OperatorRole.OPS_ADMIN.can(Permission.TENANT_STATUS_WRITE));
        assertTrue(OperatorRole.OPS_ADMIN.can(Permission.AUDIT_READ));
    }

    @Test
    @DisplayName("모든 역할이 업체를 조회할 수 있다")
    void everyoneCanReadTenants() {
        for (OperatorRole role : OperatorRole.values()) {
            assertTrue(role.can(Permission.TENANT_READ), role + " 는 업체를 조회할 수 있어야 한다");
        }
    }
}
