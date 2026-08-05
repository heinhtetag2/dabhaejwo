package com.dabhaejwo.domain.auth.entity;

/**
 * 인증 주체 종류. 업체 담당자와 운영자는 신뢰 수준이 전혀 다르므로
 * 챌린지도 어느 쪽인지 못 박는다 — 업체 챌린지로 운영자 토큰을 받아낼 수 없다.
 */
public enum AuthScope {
    APP,
    OPS
}
