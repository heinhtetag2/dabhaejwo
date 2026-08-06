package com.dabhaejwo.global.common;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * "오늘"의 경계.
 *
 * <p>시각은 전부 {@code timestamptz}(UTC)로 저장한다 — 그건 그대로 둔다. 여기서 정하는 건
 * <b>하루를 어디서 끊느냐</b>다. 업체가 보는 "오늘 대화 수"는 업체의 하루여야 한다.
 *
 * <p>UTC 로 끊으면 한국 시간 <b>오전 0시부터 9시까지의 대화가 어제로 잡힌다.</b>
 * 아침에 들어온 문의가 전날 숫자에 붙으니 업체는 화면을 못 믿게 된다.
 *
 * <p>더 나빴던 것은 {@code HomeService} 가 <b>둘을 섞고 있었다</b>는 점이다 —
 * {@code LocalDate.now()}(JVM 로컬)로 날짜를 구하고 그 자정을 UTC 로 못 박아,
 * 어느 쪽 하루도 아닌 9시간 어긋난 창이 만들어졌다.
 *
 * <p>지금은 한국 서비스라 고정값이다. 해외 법인이 생기면 업체별 타임존이 필요해지는데,
 * 그때는 이 클래스가 바뀌는 유일한 지점이 된다.
 */
public final class BusinessDay {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private BusinessDay() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** 그 날짜의 시작 시각. 저장은 UTC 지만 경계는 업체의 하루를 따른다. */
    public static OffsetDateTime startOf(LocalDate day) {
        return day.atStartOfDay(ZONE).toOffsetDateTime();
    }

    public static OffsetDateTime startOfToday() {
        return startOf(today());
    }

    /** 이번 달 1일 00:00. 월 대화 한도 판정이 쓴다. */
    public static OffsetDateTime startOfThisMonth() {
        return startOf(today().withDayOfMonth(1));
    }
}
