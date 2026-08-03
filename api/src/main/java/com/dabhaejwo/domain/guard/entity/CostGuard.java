package com.dabhaejwo.domain.guard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 비용 안전장치와 운영 임계값. 단일 행(id=1)으로 운영한다.
 *
 * <p>안전장치는 선택이 아니라 필수다 — 없으면 한 업체의 버그가 하루 매출을 삼키고,
 * 공격에 방어선이 없다 (admin-console-plan.md §4.7).
 */
@Entity
@Table(name = "cost_guards")
public class CostGuard {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(name = "tenant_daily_cap_krw", nullable = false)
    private int tenantDailyCapKrw;

    @Column(name = "global_daily_cap_krw", nullable = false)
    private int globalDailyCapKrw;

    @Column(name = "ip_questions_per_min", nullable = false)
    private int ipQuestionsPerMin;

    @Column(name = "bulk_upload_limit", nullable = false)
    private int bulkUploadLimit;

    /** 원가율 경고선. 실서버비·인건비를 반영해 재산정해야 하므로 설정값이다. */
    @Column(name = "cost_ratio_warn_percent", nullable = false)
    private int costRatioWarnPercent;

    /** 최근접 조각의 유사도가 이 값보다 낮으면 답변 실패로 처리한다. */
    @Column(name = "answer_fail_similarity", nullable = false)
    private BigDecimal answerFailSimilarity;

    @Column(name = "default_chunk_count", nullable = false)
    private int defaultChunkCount;

    @Column(name = "answer_max_length", nullable = false)
    private int answerMaxLength;

    @Column(name = "churn_purge_grace_days", nullable = false)
    private int churnPurgeGraceDays;

    @Column(name = "quota_exceeded_behavior", nullable = false)
    private String quotaExceededBehavior;

    @Column(name = "slack_alert_enabled", nullable = false)
    private boolean slackAlertEnabled;

    @Column(name = "common_prompt", nullable = false)
    private String commonPrompt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CostGuard() {
    }

    public int getTenantDailyCapKrw() {
        return tenantDailyCapKrw;
    }

    public int getGlobalDailyCapKrw() {
        return globalDailyCapKrw;
    }

    public int getIpQuestionsPerMin() {
        return ipQuestionsPerMin;
    }

    public int getBulkUploadLimit() {
        return bulkUploadLimit;
    }

    public int getCostRatioWarnPercent() {
        return costRatioWarnPercent;
    }

    public BigDecimal getAnswerFailSimilarity() {
        return answerFailSimilarity;
    }

    public int getDefaultChunkCount() {
        return defaultChunkCount;
    }

    public int getAnswerMaxLength() {
        return answerMaxLength;
    }

    public int getChurnPurgeGraceDays() {
        return churnPurgeGraceDays;
    }

    public String getCommonPrompt() {
        return commonPrompt;
    }
}
