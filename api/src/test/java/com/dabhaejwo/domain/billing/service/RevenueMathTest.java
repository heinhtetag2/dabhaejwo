package com.dabhaejwo.domain.billing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevenueMathTest {

    @Test
    @DisplayName("미수금은 청구에서 수납과 환불을 뺀 값이다")
    void outstandingSubtractsCollectedAndRefunded() {
        assertEquals(117_000, RevenueMath.outstanding(1_560_000, 1_404_000, 39_000));
    }

    @Test
    @DisplayName("환불이 청구보다 커도 미수금은 음수가 되지 않는다")
    void outstandingNeverNegative() {
        // 지난달 건을 이번 달에 환불하면 실제로 일어난다. "받을 돈이 마이너스"는 읽을 수 없는 말이다.
        assertEquals(0, RevenueMath.outstanding(39_000, 39_000, 89_000));
    }

    @Test
    @DisplayName("마진은 수납액에서 모델 원가를 뺀 값이다 — 청구액이 아니다")
    void marginUsesCollectedNotBilled() {
        assertEquals(new BigDecimal("1021699.5"),
                RevenueMath.margin(1_443_000, new BigDecimal("421300.5")));
    }

    @Test
    @DisplayName("모델 원가가 수납액을 넘으면 마진이 음수로 나온다 — 적자를 감추지 않는다")
    void marginGoesNegative() {
        assertEquals(new BigDecimal("-11000.0"),
                RevenueMath.margin(39_000, new BigDecimal("50000.0")));
        assertEquals(-28, RevenueMath.marginPercent(39_000, new BigDecimal("50000.0")));
    }

    @Test
    @DisplayName("수납액이 0이면 마진율은 null 이다 — 0% 는 '남는 게 없다'로 읽힌다")
    void marginPercentUndefinedWithoutRevenue() {
        assertNull(RevenueMath.marginPercent(0, new BigDecimal("88200.0")));
    }

    @Test
    @DisplayName("마진율은 수납액 대비 비율이다")
    void marginPercentOfCollected() {
        assertEquals(71, RevenueMath.marginPercent(1_443_000, new BigDecimal("421300.5")));
    }

    @Test
    @DisplayName("전환율은 가입 코호트 대비 비율이다")
    void conversionIsCohortBased() {
        assertEquals(38, RevenueMath.conversionPercent(24, 9));
    }

    @Test
    @DisplayName("가입이 없던 달의 전환율은 null 이다 — 0% 는 '아무도 전환 안 했다'로 읽힌다")
    void conversionUndefinedWithoutSignups() {
        assertNull(RevenueMath.conversionPercent(0, 0));
    }

    @Test
    @DisplayName("말일 가입자의 체험이 안 끝났으면 그 달 코호트는 아직 열려 있다")
    void cohortOpenUntilTrialEnds() {
        YearMonth august = YearMonth.of(2026, 8);

        // 8월 31일 가입자의 체험 종료는 9월 14일. 그 전에는 전환율이 더 오를 수 있다.
        assertTrue(RevenueMath.cohortOpen(august, LocalDate.of(2026, 9, 14)));
        assertFalse(RevenueMath.cohortOpen(august, LocalDate.of(2026, 9, 15)));
    }

    @Test
    @DisplayName("한참 지난 달의 코호트는 닫혀 있다")
    void oldCohortIsClosed() {
        assertFalse(RevenueMath.cohortOpen(YearMonth.of(2026, 1), LocalDate.of(2026, 8, 6)));
    }
}
