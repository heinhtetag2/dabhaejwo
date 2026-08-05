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
    COST_GUARD_WRITE(true),

    /** 일시정지 해제. 사유를 요구하지 않는 이유는 정지 사유가 이미 남아 있기 때문이다. */
    ACTIVATE(false),
    /** 요금제 <b>정의</b> 수정. 특정 업체의 요금제 변경은 {@link #CHANGE_PLAN} 이다. */
    PLAN_WRITE(false),
    FLAG_WRITE(false),
    TICKET_WRITE(false),
    /**
     * 공급사 API 키 등록·교체·중지. 사유를 요구하는 이유는 단가·안전장치와 같다 —
     * 전 업체의 답변을 즉시 멈추거나 되살릴 수 있는 조작이다.
     *
     * <p>기록에 키나 그 힌트를 남기지 않는다. 남는 것은 "누가 왜 어느 공급사를 만졌나"뿐이다.
     */
    PROVIDER_CREDENTIAL_WRITE(true),

    /**
     * 운영자 계정 등록·역할 변경·비활성화·비밀번호 재설정.
     *
     * <p><b>권한 자체를 바꾸는 행위</b>라 사유를 요구한다. 누가 누구에게 무슨 역할을 줬는지
     * 남지 않으면 "언제부터 이 사람이 감사 기록을 볼 수 있었나"에 답할 수 없다.
     *
     * <p>기록에 비밀번호는 남기지 않는다.
     */
    OPERATOR_WRITE(true);

    private final boolean reasonRequired;

    AuditAction(boolean reasonRequired) {
        this.reasonRequired = reasonRequired;
    }

    public boolean reasonRequired() {
        return reasonRequired;
    }
}
