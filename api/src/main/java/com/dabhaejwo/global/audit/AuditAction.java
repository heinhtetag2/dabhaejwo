package com.dabhaejwo.global.audit;

/**
 * 감사 대상 행위. 값은 audit_logs.action 의 CHECK 제약과 일치해야 한다.
 *
 * <p>{@code reasonRequired} 가 true 인 행위는 사유 없이 실행되지 않는다 —
 * 검증은 프론트가 아니라 서비스 레이어에서 한다.
 */
public enum AuditAction {

    /** 남의 고객 데이터를 그대로 열람하는 행위. */
    IMPERSONATE(true),
    VIEW_CONVERSATIONS(true),
    CHANGE_PLAN(true),
    GRANT_QUOTA(true),
    SUSPEND(true),
    CHURN(true),
    EXTEND_TRIAL(false),
    MODEL_PRICE_WRITE(true),
    COST_GUARD_WRITE(true);

    private final boolean reasonRequired;

    AuditAction(boolean reasonRequired) {
        this.reasonRequired = reasonRequired;
    }

    public boolean reasonRequired() {
        return reasonRequired;
    }
}
