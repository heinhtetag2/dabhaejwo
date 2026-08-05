package com.dabhaejwo.domain.job.dto.response;

/**
 * 작업 큐 지표 4종. api-contracts.md §8.
 *
 * @param successPercent 오늘 완료 대비 성공률. 오늘 처리된 작업이 하나도 없으면 {@code null} 이다 —
 *                       0% 로 내려보내면 "전부 실패했다"로 읽힌다.
 */
public record JobStatsResponse(
        long queuedCount,
        long runningCount,
        long doneTodayCount,
        Double successPercent,
        long failedCount) {
}
