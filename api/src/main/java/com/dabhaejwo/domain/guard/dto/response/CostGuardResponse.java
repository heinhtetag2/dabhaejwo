package com.dabhaejwo.domain.guard.dto.response;

import com.dabhaejwo.domain.guard.entity.CostGuard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 비용 안전장치 + 운영 임계값. api-contracts.md §7.
 *
 * <p>안전장치는 선택이 아니라 필수다 — 없으면 한 업체의 버그가 하루 매출을 삼키고,
 * 공격에 방어선이 없다 (admin-console-plan.md §4.7).
 */
public record CostGuardResponse(
        int tenantDailyCapKrw,
        int globalDailyCapKrw,
        int ipQuestionsPerMin,
        int bulkUploadLimit,
        int costRatioWarnPercent,
        BigDecimal answerFailSimilarity,
        int defaultChunkCount,
        int answerMaxLength,
        int churnPurgeGraceDays,
        String quotaExceededBehavior,
        boolean slackAlertEnabled,
        String commonPrompt,
        /** 문서·질문을 임베딩할 공급사. 바꾸면 기존 조각이 무효가 되어 다시 학습해야 한다. */
        String embeddingProvider,
        OffsetDateTime updatedAt) {

    public static CostGuardResponse from(CostGuard guard) {
        return new CostGuardResponse(
                guard.getTenantDailyCapKrw(),
                guard.getGlobalDailyCapKrw(),
                guard.getIpQuestionsPerMin(),
                guard.getBulkUploadLimit(),
                guard.getCostRatioWarnPercent(),
                guard.getAnswerFailSimilarity(),
                guard.getDefaultChunkCount(),
                guard.getAnswerMaxLength(),
                guard.getChurnPurgeGraceDays(),
                guard.getQuotaExceededBehavior(),
                guard.isSlackAlertEnabled(),
                guard.getCommonPrompt(),
                guard.getEmbeddingProvider(),
                guard.getUpdatedAt());
    }
}
