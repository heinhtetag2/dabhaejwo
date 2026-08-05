package com.dabhaejwo.domain.flag.entity;

/**
 * 공개 대상. 값은 {@code feature_flags.scope} 의 CHECK 제약과 일치해야 한다.
 *
 * <p>기능 플래그는 <b>코드 배포와 기능 공개를 분리</b>한다. 문제가 생기면 배포를 되돌리지
 * 않고 플래그만 끈다 (admin-console-plan.md §4.8).
 */
public enum FlagScope {
    /** 개발 중. 내부 테스트만. */
    INTERNAL,
    /** 베타 — 협조적인 업체 소수. */
    TENANTS,
    /** 상위 플랜 전용 기능. */
    PLAN,
    /** 정식 출시. */
    ALL
}
