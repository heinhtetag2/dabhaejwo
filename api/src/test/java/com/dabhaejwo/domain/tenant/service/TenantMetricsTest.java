package com.dabhaejwo.domain.tenant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 저장 답변 비율은 수익성 화면에서 원가율 옆에 나란히 놓이는 값이다 —
 * 두 열의 상관관계가 "공통 질문을 등록하세요"라는 안내의 근거가 된다.
 * 계산이 틀리면 그 근거가 통째로 무너진다.
 */
class TenantMetricsTest {

    @Test
    @DisplayName("저장 답변 비율의 분모는 대화 수 + 저장 답변 수다")
    void savedAnswerPercentIncludesSavedInDenominator() {
        // 저장 답변은 모델을 거치지 않아 대화 사용량(convCount)에 잡히지 않는다.
        // 분모를 convCount 로만 잡으면 100%를 넘는 비율이 나온다.
        var metrics = new TenantMetrics(61, 39, new BigDecimal("1000"));

        assertEquals(39, metrics.savedAnswerPercent());
    }

    @Test
    @DisplayName("응답이 한 건도 없으면 0이다 — 0으로 나누지 않는다")
    void returnsZeroWhenNoResponses() {
        assertEquals(0, TenantMetrics.empty().savedAnswerPercent());
    }

    @Test
    @DisplayName("전부 저장 답변이면 100%다")
    void allSaved() {
        var metrics = new TenantMetrics(0, 12, BigDecimal.ZERO);

        assertEquals(100, metrics.savedAnswerPercent());
    }

    @Test
    @DisplayName("집계가 없는 업체는 0으로 다룬다 — null 이 호출부로 새지 않는다")
    void nullTotalBecomesEmpty() {
        var metrics = TenantMetrics.from(null);

        assertEquals(0, metrics.convCount());
        assertEquals(BigDecimal.ZERO, metrics.costKrw());
    }
}
