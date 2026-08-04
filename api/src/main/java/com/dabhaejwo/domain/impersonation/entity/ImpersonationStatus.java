package com.dabhaejwo.domain.impersonation.entity;

public enum ImpersonationStatus {
    ACTIVE,
    ENDED,
    EXPIRED,
    /** 대상 업체가 해지되어 강제 종료됐다. */
    REVOKED
}
