package com.dabhaejwo.domain.tenant.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 청구 기준일의 월말 처리.
 *
 * <p>"결제한 날 기준"은 31일에 결제한 업체에서 곧바로 무너진다 — 2월 31일이 없기 때문이다.
 * 여기서 틀리면 <b>청구일이 매년 조금씩 앞당겨지는데</b> 아무도 눈치채지 못한다.
 */
class BillingDayTest {

    @Test
    @DisplayName("있는 날짜는 그대로 쓴다")
    void 평범한_날() {
        assertThat(Tenant.onDay(LocalDate.of(2026, 9, 1), 3))
                .isEqualTo(LocalDate.of(2026, 9, 3));
    }

    @Test
    @DisplayName("31일 기준이 2월에는 말일로 당겨진다")
    void 이월_말일() {
        assertThat(Tenant.onDay(LocalDate.of(2026, 2, 1), 31))
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("윤년 2월은 29일이다")
    void 윤년() {
        assertThat(Tenant.onDay(LocalDate.of(2028, 2, 1), 31))
                .isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("2월을 지나도 기준일로 되돌아온다 — 이게 핵심이다")
    void 삼월에_되돌아온다() {
        // 다음 결제일에서 한 달을 더하는 방식이면 2/28 → 3/28 로 눌러앉는다.
        // 기준일(31)을 따로 들고 있어야 3월에 31일로 돌아온다.
        assertThat(Tenant.onDay(LocalDate.of(2026, 3, 1), 31))
                .isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("30일 기준도 2월에는 당겨지고 4월에는 그대로다")
    void 삼십일_기준() {
        assertThat(Tenant.onDay(LocalDate.of(2026, 2, 1), 30))
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(Tenant.onDay(LocalDate.of(2026, 4, 1), 30))
                .isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("기준일이 없으면 받은 날짜를 그대로 둔다")
    void 기준일_없음() {
        assertThat(Tenant.onDay(LocalDate.of(2026, 5, 17), null))
                .isEqualTo(LocalDate.of(2026, 5, 17));
    }

    @Test
    @DisplayName("정지하면 청구를 멈추고, 해제하면 되살린다")
    void 정지와_해제() {
        Tenant tenant = Tenant.startTrial("가게", "shop.example.com", "pk_live_x", UUID.randomUUID(), 14);
        tenant.changePlan(UUID.randomUUID());
        tenant.activate();
        tenant.startBillingOn(LocalDate.of(2026, 8, 3));

        tenant.suspend();
        // 정지된 업체를 계속 청구하면 매일 실패만 쌓인다.
        assertThat(tenant.getNextBillingDate()).isNull();

        tenant.activate();
        // 여기서 되살리지 않으면 서비스는 쓰면서 돈은 안 내는 상태가 된다.
        assertThat(tenant.getNextBillingDate()).isNotNull();
        assertThat(tenant.getBillingDay()).isEqualTo(3);
    }
}
