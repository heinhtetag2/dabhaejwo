package com.dabhaejwo.domain.tenant.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 상태 전이는 성공/거부 양쪽을 모두 고정한다 (core workflow-rules). */
class TenantStatusTest {

    @Test
    @DisplayName("체험은 활성 또는 해지로만 갈 수 있다")
    void trialTransitions() {
        assertTrue(TenantStatus.TRIAL.canTransitionTo(TenantStatus.ACTIVE));
        assertTrue(TenantStatus.TRIAL.canTransitionTo(TenantStatus.CHURNED));
        assertFalse(TenantStatus.TRIAL.canTransitionTo(TenantStatus.SUSPENDED));
    }

    @Test
    @DisplayName("일시정지는 활성으로 되돌릴 수 있다")
    void suspendedCanReactivate() {
        assertTrue(TenantStatus.ACTIVE.canTransitionTo(TenantStatus.SUSPENDED));
        assertTrue(TenantStatus.SUSPENDED.canTransitionTo(TenantStatus.ACTIVE));
    }

    @Test
    @DisplayName("해지는 종착이다 — 어떤 상태로도 나갈 수 없다")
    void churnedIsTerminal() {
        for (TenantStatus target : TenantStatus.values()) {
            assertFalse(TenantStatus.CHURNED.canTransitionTo(target),
                    "CHURNED → " + target + " 는 허용되면 안 된다");
        }
    }

    @Test
    @DisplayName("자기 자신으로의 전이는 허용하지 않는다")
    void noSelfTransition() {
        for (TenantStatus status : TenantStatus.values()) {
            assertFalse(status.canTransitionTo(status),
                    status + " → " + status + " 는 허용되면 안 된다");
        }
    }

    @Test
    @DisplayName("체험·활성만 방문자에게 응답한다")
    void onlyLiveStatusesServeVisitors() {
        assertTrue(TenantStatus.TRIAL.servesVisitors());
        assertTrue(TenantStatus.ACTIVE.servesVisitors());
        assertFalse(TenantStatus.SUSPENDED.servesVisitors());
        assertFalse(TenantStatus.CHURNED.servesVisitors());
    }
}
