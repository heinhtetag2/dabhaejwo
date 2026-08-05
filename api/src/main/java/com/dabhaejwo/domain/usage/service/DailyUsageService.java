package com.dabhaejwo.domain.usage.service;

import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 일 집계 테이블 조회. 오늘·업체 목록·수익성 화면이 여기를 읽는다 —
 * {@code ai_usage} 를 직접 집계하면 업체 수와 기간에 비례해 느려진다
 * (admin-console-plan.md §6.1).
 */
@Service
public class DailyUsageService {

    private final TenantDailyUsageRepository repository;

    public DailyUsageService(TenantDailyUsageRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Total today() {
        LocalDate today = LocalDate.now();
        TenantDailyUsageRepository.GlobalTotal total = repository.totalBetween(today, today);
        if (total == null) {
            return new Total(0, 0, BigDecimal.ZERO);
        }
        return new Total(total.getConvCount(), total.getSavedCount(),
                total.getCostKrw() == null ? BigDecimal.ZERO : total.getCostKrw());
    }

    /**
     * 오늘분 마지막 집계 시각. 한 번도 집계된 적 없으면 null 이다.
     *
     * <p>화면이 "몇 시 기준"인지 밝히기 위한 값이다 — 집계가 언제 것인지 모르면
     * 숫자를 믿을 수 없다.
     */
    @Transactional(readOnly = true)
    public OffsetDateTime lastAggregatedAt() {
        return repository.lastAggregatedAt(LocalDate.now());
    }

    public record Total(long convCount, long savedCount, BigDecimal costKrw) {
    }
}
