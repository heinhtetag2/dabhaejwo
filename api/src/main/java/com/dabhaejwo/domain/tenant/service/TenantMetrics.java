package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 업체 한 곳의 이번 달 집계. {@code tenant_daily_usage} 합계를 담는다.
 *
 * <p>집계가 없는 업체(이번 달 대화가 한 건도 없는 업체)를 null 로 다루면 호출부마다
 * null 검사가 흩어진다. {@link #empty()} 로 0 을 표현한다.
 */
public record TenantMetrics(long convCount, long savedCount, BigDecimal costKrw) {

    public static TenantMetrics empty() {
        return new TenantMetrics(0, 0, BigDecimal.ZERO);
    }

    public static TenantMetrics from(TenantDailyUsageRepository.MonthlyTotal total) {
        if (total == null) {
            return empty();
        }
        return new TenantMetrics(
                total.getConvCount(),
                total.getSavedCount(),
                total.getCostKrw() == null ? BigDecimal.ZERO : total.getCostKrw());
    }

    /**
     * 저장 답변 비율. 저장 답변은 모델을 거치지 않아 원가가 0 이고 대화 사용량에도 잡히지 않으므로
     * (api-contracts.md §10) 분모는 둘의 합이다.
     *
     * <p>응답이 한 건도 없으면 0 이다. 0으로 나누지 않는다.
     */
    public int savedAnswerPercent() {
        long total = convCount + savedCount;
        if (total <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(savedCount * 100L)
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
