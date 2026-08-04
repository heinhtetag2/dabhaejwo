package com.dabhaejwo.domain.gap.entity;

public enum GapStatus {
    OPEN,
    /** 답을 등록해 FAQ 로 승격됐다. */
    RESOLVED,
    /** 업체가 목록에서 감췄다. 다시 물어보면 OPEN 으로 되살아난다. */
    DISMISSED
}
