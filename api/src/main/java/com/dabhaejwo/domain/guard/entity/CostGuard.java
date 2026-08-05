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

    /**
     * 문서·질문을 임베딩할 공급사.
     *
     * <p><b>바꾸면 기존 조각이 전부 무효가 된다</b> — 다른 모델이 만든 벡터끼리는 거리를
     * 비교할 수 없다. 그래서 문서마다 학습 출처를 남기고, 설정과 다른 문서를 다시 학습
     * 대상으로 표시한다.
     */
    @Column(name = "embedding_provider", nullable = false)
    private String embeddingProvider;

    @Column(name = "slack_alert_enabled", nullable = false)
    private boolean slackAlertEnabled;

    @Column(name = "common_prompt", nullable = false)
    private String commonPrompt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CostGuard() {
    }

    /**
     * 안전장치 갱신. 단일 행이므로 생성이 없고 수정만 있다.
     *
     * <p>상한을 0 이하로 두는 것을 막는다 — 0 은 "제한 없음"이 아니라 "전부 차단"으로
     * 동작하고, 그 상태는 화면에서 정상처럼 보인다.
     */
    public void update(int tenantDailyCap,
                       int globalDailyCap,
                       int ipPerMin,
                       int bulkLimit,
                       int warnPercent,
                       BigDecimal failSimilarity,
                       int chunkCount,
                       int maxLength,
                       int purgeGraceDays,
                       String behavior,
                       boolean slackEnabled,
                       String prompt) {
        this.tenantDailyCapKrw = tenantDailyCap;
        this.globalDailyCapKrw = globalDailyCap;
        this.ipQuestionsPerMin = ipPerMin;
        this.bulkUploadLimit = bulkLimit;
        this.costRatioWarnPercent = warnPercent;
        this.answerFailSimilarity = failSimilarity;
        this.defaultChunkCount = chunkCount;
        this.answerMaxLength = maxLength;
        this.churnPurgeGraceDays = purgeGraceDays;
        this.quotaExceededBehavior = behavior;
        this.slackAlertEnabled = slackEnabled;
        this.commonPrompt = prompt;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * 임베딩 공급사 교체.
     *
     * <p>{@link #update} 와 <b>따로 두는 것이 의도다.</b> 이 값은 바꾸는 순간 기존 조각을
     * 전부 무효로 만든다 — 슬랙 알림 토글과 같은 저장 버튼에 묶이면 사고가 난다.
     */
    public void changeEmbeddingProvider(String provider) {
        this.embeddingProvider = provider;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getQuotaExceededBehavior() {
        return quotaExceededBehavior;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public boolean isSlackAlertEnabled() {
        return slackAlertEnabled;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
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
