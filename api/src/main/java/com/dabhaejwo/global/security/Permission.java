package com.dabhaejwo.global.security;

/** 권한 키는 {RESOURCE}_{ACTION} (core security-rules). */
public enum Permission {

    TENANT_READ,
    TENANT_NOTE_WRITE,
    TENANT_STATUS_WRITE,
    TENANT_PLAN_WRITE,
    TENANT_TRIAL_WRITE,
    TENANT_IMPERSONATE,
    QUOTA_GRANT,

    PROFITABILITY_READ,
    AI_USAGE_READ,

    JOB_READ,
    JOB_RETRY,

    PLAN_READ,
    PLAN_WRITE,
    MODEL_PRICE_READ,
    MODEL_PRICE_WRITE,
    COST_GUARD_READ,
    COST_GUARD_WRITE,

    /**
     * 공급사 API 키 조회·등록. <b>OPS_ADMIN 전용이다</b> — 키를 바꾸면 전 업체의 답변이
     * 즉시 멈추거나 남의 계정으로 과금이 흘러갈 수 있다.
     */
    PROVIDER_CREDENTIAL_READ,
    PROVIDER_CREDENTIAL_WRITE,

    /**
     * 운영자 계정 관리. <b>OPS_ADMIN 전용이다</b> — 이 권한이 있으면 자기 역할을 포함해
     * 누구의 권한이든 바꿀 수 있으므로, 다른 역할에 주면 권한 체계가 무의미해진다.
     */
    OPERATOR_READ,
    OPERATOR_WRITE,

    FLAG_READ,
    FLAG_WRITE,

    TICKET_READ,
    TICKET_WRITE,

    AUDIT_READ
}
