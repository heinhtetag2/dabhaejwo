package com.dabhaejwo.global.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 원가 계산은 이 서비스에서 가장 되돌리기 어려운 로직이다 — 틀린 값이 원장에 확정 저장되면
 * 나중에 고칠 수 없다. Docker 가 없어 통합 테스트를 못 도는 환경이므로
 * 계산 로직을 순수 단위 테스트로 고정한다 (docs/IMPROVEMENTS.md P1).
 */
class ModelPriceLookupTest {

    private ModelPriceLookup.ResolvedPrice price(String inputPer1m, String outputPer1m) {
        return new ModelPriceLookup.ResolvedPrice(
                1L,
                new BigDecimal(inputPer1m),
                outputPer1m == null ? null : new BigDecimal(outputPer1m));
    }

    @Test
    @DisplayName("입력·출력 토큰 원가를 합산한다")
    void computesInputAndOutputCost() {
        // 1,000,000 입력 토큰 × 2100원 + 1,000,000 출력 토큰 × 12600원
        var resolved = price("2100.00", "12600.00");

        assertEquals(new BigDecimal("14700.0000"), resolved.costOf(1_000_000, 1_000_000));
    }

    @Test
    @DisplayName("임베딩 모델은 출력 단가가 없어도 입력 원가만으로 계산된다")
    void handlesNullOutputPrice() {
        var resolved = price("210.00", null);

        assertEquals(new BigDecimal("210.0000"), resolved.costOf(1_000_000, 0));
    }

    @Test
    @DisplayName("소수 4자리를 유지한다 — 반올림하면 누적 오차가 난다")
    void keepsFourDecimalPlaces() {
        var resolved = price("2100.00", "12600.00");

        // 100 토큰이면 0.21원. 원 단위로 반올림하면 0원이 되어 원장에 구멍이 난다.
        assertEquals(new BigDecimal("0.2100"), resolved.costOf(100, 0));
    }

    @Test
    @DisplayName("출력 토큰이 0이면 출력 원가를 더하지 않는다")
    void ignoresZeroOutputTokens() {
        var resolved = price("2100.00", "12600.00");

        assertEquals(new BigDecimal("2100.0000"), resolved.costOf(1_000_000, 0));
    }
}
