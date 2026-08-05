package com.dabhaejwo.domain.usage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 일 단위 집계.
 *
 * <p>원가율은 매 요청마다 ai_usage 를 집계하면 업체 수에 비례해 느려진다.
 * 오늘·업체 목록·수익성 화면은 이 테이블만 읽는다 (admin-console-plan.md §6.1).
 */
@Entity
@Table(name = "tenant_daily_usage")
@IdClass(TenantDailyUsage.Key.class)
public class TenantDailyUsage {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    private LocalDate day;

    @Column(name = "conv_count", nullable = false)
    private int convCount;

    /** 저장 답변으로 처리돼 모델 원가가 발생하지 않은 건수. */
    @Column(name = "saved_count", nullable = false)
    private int savedCount;

    @Column(name = "doc_count", nullable = false)
    private int docCount;

    @Column(name = "tokens_in", nullable = false)
    private long tokensIn;

    @Column(name = "tokens_out", nullable = false)
    private long tokensOut;

    @Column(name = "cost_krw", nullable = false)
    private BigDecimal costKrw;

    /** 당일분은 이 시각을 화면에 함께 표시한다 — 집계가 언제 것인지 모르면 숫자를 믿을 수 없다. */
    @Column(name = "aggregated_at", nullable = false)
    private OffsetDateTime aggregatedAt;

    protected TenantDailyUsage() {
    }

    public static TenantDailyUsage of(UUID tenantId, LocalDate day) {
        TenantDailyUsage usage = new TenantDailyUsage();
        usage.tenantId = tenantId;
        usage.day = day;
        usage.costKrw = BigDecimal.ZERO;
        usage.aggregatedAt = OffsetDateTime.now();
        return usage;
    }

    /**
     * 배치가 다시 계산한 값으로 덮는다. <b>더하지 않는다</b> — 증분으로 더하면 배치가
     * 두 번 돌거나 중간에 실패했을 때 숫자가 부풀고, 그 사실을 알아챌 방법이 없다.
     * 원장({@code ai_usage})이 진실이고 이 테이블은 캐시다.
     */
    public void overwrite(long newConvCount, long newSavedCount,
                          long newTokensIn, long newTokensOut, BigDecimal newCostKrw) {
        this.convCount = (int) newConvCount;
        this.savedCount = (int) newSavedCount;
        this.tokensIn = newTokensIn;
        this.tokensOut = newTokensOut;
        this.costKrw = newCostKrw == null ? BigDecimal.ZERO : newCostKrw;
        this.aggregatedAt = OffsetDateTime.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public LocalDate getDay() {
        return day;
    }

    public int getConvCount() {
        return convCount;
    }

    public int getSavedCount() {
        return savedCount;
    }

    public BigDecimal getCostKrw() {
        return costKrw;
    }

    public OffsetDateTime getAggregatedAt() {
        return aggregatedAt;
    }

    public static class Key implements Serializable {

        private UUID tenantId;
        private LocalDate day;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(tenantId, key.tenantId) && Objects.equals(day, key.day);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, day);
        }
    }
}
