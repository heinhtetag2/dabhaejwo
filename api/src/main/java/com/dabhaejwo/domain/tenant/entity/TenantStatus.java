package com.dabhaejwo.domain.tenant.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * 업체 상태와 허용 전이.
 *
 * <p>허용 전이를 코드로 먼저 정의하고, 모든 상태 변경이 이를 통과하게 한다.
 * "일단 값만 바꾸기"를 막기 위한 것이다 (core workflow-rules).
 *
 * <pre>
 * TRIAL     → ACTIVE | CHURNED
 * ACTIVE    → SUSPENDED | CHURNED
 * SUSPENDED → ACTIVE | CHURNED
 * CHURNED   → (종착)
 * </pre>
 */
public enum TenantStatus {

    TRIAL,
    ACTIVE,
    SUSPENDED,
    /** 해지는 종착 상태다. 복귀는 신규 계약으로 처리한다. */
    CHURNED;

    private static final Set<TenantStatus> FROM_TRIAL = EnumSet.of(ACTIVE, CHURNED);
    private static final Set<TenantStatus> FROM_ACTIVE = EnumSet.of(SUSPENDED, CHURNED);
    private static final Set<TenantStatus> FROM_SUSPENDED = EnumSet.of(ACTIVE, CHURNED);

    public boolean canTransitionTo(TenantStatus target) {
        return switch (this) {
            case TRIAL -> FROM_TRIAL.contains(target);
            case ACTIVE -> FROM_ACTIVE.contains(target);
            case SUSPENDED -> FROM_SUSPENDED.contains(target);
            case CHURNED -> false;
        };
    }

    /** 챗봇이 방문자에게 응답해도 되는 상태인가. */
    public boolean servesVisitors() {
        return this == TRIAL || this == ACTIVE;
    }
}
