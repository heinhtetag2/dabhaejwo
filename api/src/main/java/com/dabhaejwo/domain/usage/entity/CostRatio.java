package com.dabhaejwo.domain.usage.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 원가율 = 이번 달 모델 원가 ÷ 이번 달 청구액 × 100.
 *
 * <p>파생 컬럼으로 저장하지 않고 집계에서 계산한다.
 * 경고선(기본 70%)은 {@code cost_guards} 설정값이다 — 실서버비·인건비를 반영해
 * 재산정해야 하므로 코드에 박아두지 않는다.
 */
public record CostRatio(int percent, Level level) {

    public enum Level {
        /** 정상. */
        NORMAL,
        /** 주의 — 인건비·서버비를 빼면 남는 게 거의 없다. */
        WARN,
        /** 손실 — 쓸수록 적자. */
        LOSS
    }

    public static final int LOSS_THRESHOLD_PERCENT = 100;

    public static CostRatio of(BigDecimal costKrw, int billedKrw, int warnThresholdPercent) {
        if (billedKrw <= 0) {
            // 무료 체험·협의가 업체는 청구액이 없어 원가율을 정의할 수 없다.
            // 0%로 표시하면 정상처럼 보이므로 별도로 다룬다.
            return new CostRatio(0, Level.NORMAL);
        }
        int percent = costKrw
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(billedKrw), 0, RoundingMode.HALF_UP)
                .intValue();
        return new CostRatio(percent, levelOf(percent, warnThresholdPercent));
    }

    private static Level levelOf(int percent, int warnThresholdPercent) {
        if (percent >= LOSS_THRESHOLD_PERCENT) {
            return Level.LOSS;
        }
        if (percent >= warnThresholdPercent) {
            return Level.WARN;
        }
        return Level.NORMAL;
    }

    /** 막대는 100%에서 멈추고 초과분은 숫자로만 표기한다 (tenant-plan.md §4.1.4). */
    public int barPercent() {
        return Math.min(percent, 100);
    }
}
