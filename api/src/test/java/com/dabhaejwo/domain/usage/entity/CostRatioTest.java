package com.dabhaejwo.domain.usage.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 원가율은 운영자가 매일 가장 먼저 보는 숫자다. 구간 경계를 테스트로 고정한다.
 * 값은 docs/prototype/chatbot-admin-console.html 의 샘플과 맞춰 두었다.
 */
class CostRatioTest {

    private static final int WARN = 70;

    @Test
    @DisplayName("스튜디오 하우스: 39,000원에 원가 71,200원이면 183% 손실")
    void lossCase() {
        CostRatio ratio = CostRatio.of(new BigDecimal("71200"), 39000, WARN);

        assertEquals(183, ratio.percent());
        assertEquals(CostRatio.Level.LOSS, ratio.level());
    }

    @Test
    @DisplayName("노르드하임: 89,000원에 원가 24,100원이면 27% 정상")
    void normalCase() {
        CostRatio ratio = CostRatio.of(new BigDecimal("24100"), 89000, WARN);

        assertEquals(27, ratio.percent());
        assertEquals(CostRatio.Level.NORMAL, ratio.level());
    }

    @Test
    @DisplayName("경고선 정확히 70%는 주의 구간에 포함된다")
    void warnBoundaryIsInclusive() {
        CostRatio ratio = CostRatio.of(new BigDecimal("70000"), 100000, WARN);

        assertEquals(70, ratio.percent());
        assertEquals(CostRatio.Level.WARN, ratio.level());
    }

    @Test
    @DisplayName("69%는 아직 정상이다")
    void justBelowWarnIsNormal() {
        assertEquals(CostRatio.Level.NORMAL,
                CostRatio.of(new BigDecimal("69000"), 100000, WARN).level());
    }

    @Test
    @DisplayName("정확히 100%부터 손실이다")
    void lossBoundaryIsInclusive() {
        assertEquals(CostRatio.Level.LOSS,
                CostRatio.of(new BigDecimal("100000"), 100000, WARN).level());
    }

    @Test
    @DisplayName("막대는 100%에서 멈추고 초과분은 숫자로만 남는다")
    void barStopsAtHundred() {
        CostRatio ratio = CostRatio.of(new BigDecimal("71200"), 39000, WARN);

        assertEquals(183, ratio.percent());
        assertEquals(100, ratio.barPercent());
    }

    @Test
    @DisplayName("청구액이 없는 업체는 원가율을 정의하지 않는다")
    void zeroBilledIsUndefined() {
        CostRatio ratio = CostRatio.of(new BigDecimal("5000"), 0, WARN);

        assertEquals(0, ratio.percent());
        assertEquals(CostRatio.Level.NORMAL, ratio.level());
    }
}
