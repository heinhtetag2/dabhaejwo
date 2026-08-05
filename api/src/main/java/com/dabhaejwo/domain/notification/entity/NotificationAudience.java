package com.dabhaejwo.domain.notification.entity;

/**
 * 알림을 누가 보는가.
 *
 * <p>운영자와 업체는 신뢰 수준이 전혀 다르다 — 토큰 체계도 필터 체인도 나뉘어 있다.
 * 알림도 같은 경계를 따른다. 이 값이 섞이면 업체가 다른 업체의 원가를 보게 된다.
 */
public enum NotificationAudience {
    OPS,
    TENANT
}
