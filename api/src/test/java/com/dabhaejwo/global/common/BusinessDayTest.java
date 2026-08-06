package com.dabhaejwo.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "오늘"의 경계.
 *
 * <p>UTC 로 끊으면 한국 시간 오전 0~9시의 대화가 어제로 잡힌다. 화면에 찍히는 숫자가
 * 조용히 틀리는 종류라 테스트로 고정한다.
 */
class BusinessDayTest {

    @Test
    @DisplayName("하루는 한국 시간 자정에 시작한다 — UTC 로는 전날 15시다")
    void 하루_시작() {
        OffsetDateTime start = BusinessDay.startOf(LocalDate.of(2026, 8, 5));

        assertThat(start.toInstant())
                .isEqualTo(OffsetDateTime.of(2026, 8, 4, 15, 0, 0, 0, ZoneOffset.UTC).toInstant());
    }

    @Test
    @DisplayName("한국 시간 오전 8시는 그날에 들어간다 — UTC 기준이면 어제로 새어나갔다")
    void 이른_아침이_오늘에_들어간다() {
        OffsetDateTime start = BusinessDay.startOf(LocalDate.of(2026, 8, 5));
        OffsetDateTime end = BusinessDay.startOf(LocalDate.of(2026, 8, 6));

        // 2026-08-05 08:00 KST == 2026-08-04 23:00 UTC
        OffsetDateTime morning =
                OffsetDateTime.of(2026, 8, 4, 23, 0, 0, 0, ZoneOffset.UTC);

        assertThat(morning).isAfterOrEqualTo(start).isBefore(end);
    }

    @Test
    @DisplayName("한국 시간 자정 직전은 아직 그날이다")
    void 자정_직전() {
        OffsetDateTime end = BusinessDay.startOf(LocalDate.of(2026, 8, 6));
        // 2026-08-05 23:59 KST == 2026-08-05 14:59 UTC
        OffsetDateTime lateNight =
                OffsetDateTime.of(2026, 8, 5, 14, 59, 0, 0, ZoneOffset.UTC);

        assertThat(lateNight).isBefore(end);
    }

    @Test
    @DisplayName("이번 달은 1일 자정부터다")
    void 이번_달() {
        assertThat(BusinessDay.startOfThisMonth().toLocalDate().getDayOfMonth()).isEqualTo(1);
        assertThat(BusinessDay.startOfThisMonth()).isBeforeOrEqualTo(BusinessDay.startOfToday());
    }
}
