package com.dabhaejwo.domain.usage.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 일 집계는 배치가 여러 번 돌 수 있다 — 매시 정각에 그날 것을 다시 계산하고,
 * 어제 것도 한 번 더 돌린다. 증분으로 더하면 돌 때마다 숫자가 부풀고,
 * 부풀었다는 사실을 알아챌 방법이 없다.
 */
class TenantDailyUsageTest {

    @Test
    @DisplayName("다시 집계하면 더하지 않고 덮는다")
    void overwriteIsNotAdditive() {
        var usage = TenantDailyUsage.of(UUID.randomUUID(), LocalDate.of(2026, 8, 4));

        usage.overwrite(100, 40, 1_000, 200, new BigDecimal("1234.56"));
        usage.overwrite(120, 45, 1_200, 240, new BigDecimal("1500.00"));

        assertEquals(120, usage.getConvCount());
        assertEquals(45, usage.getSavedCount());
        assertEquals(new BigDecimal("1500.00"), usage.getCostKrw());
    }

    @Test
    @DisplayName("원가가 없는 날은 0원이다 — null 이 테이블에 들어가지 않는다")
    void nullCostBecomesZero() {
        var usage = TenantDailyUsage.of(UUID.randomUUID(), LocalDate.of(2026, 8, 4));

        usage.overwrite(3, 0, 0, 0, null);

        assertEquals(BigDecimal.ZERO, usage.getCostKrw());
    }
}
