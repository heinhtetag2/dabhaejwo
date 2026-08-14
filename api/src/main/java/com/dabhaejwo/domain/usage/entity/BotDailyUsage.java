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
 * <p>업체 축({@link TenantDailyUsage})과 <b>따로 둔다.</b> 서비스를 PK 에 끼우고 합계로
 * 롤업하면 <b>서비스를 지웠을 때 과거 업체 매출·원가 집계가 함께 사라진다</b> —
 * 그 테이블은 계약 단위 화면(오늘·수익성·업체 목록)의 캐시이고, 계약은 서비스보다 오래 산다.
 *
 * <p>같은 배치가 두 축을 각각 채운다.
 */
@Entity
@Table(name = "bot_daily_usage")
@IdClass(BotDailyUsage.Key.class)
public class BotDailyUsage {

    @Id
    @Column(name = "bot_id")
    private UUID botId;

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

    protected BotDailyUsage() {
    }

    public static BotDailyUsage of(UUID botId, LocalDate day) {
        BotDailyUsage usage = new BotDailyUsage();
        usage.botId = botId;
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
        return botId;
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

        private UUID botId;
        private LocalDate day;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(botId, key.botId) && Objects.equals(day, key.day);
        }

        @Override
        public int hashCode() {
            return Objects.hash(botId, day);
        }
    }
}
