package com.dabhaejwo.domain.knowledge.entity;

/**
 * 문서 학습 상태.
 *
 * <p>{@code EXCLUDED} 는 업체가 "이 페이지는 학습하지 않겠다"고 뺀 것이다 — 실패가 아니다.
 * 화면의 학습 완료/처리 중/실패 3분류에는 들어가지 않는다.
 */
public enum DocumentStatus {
    PENDING,
    PROCESSING,
    INDEXED,
    FAILED,
    EXCLUDED
}
