package com.dabhaejwo.domain.app.dto.response;

import java.util.List;

/**
 * 업체 대시보드 홈. api-contracts.md §9-4.
 *
 * <p>화면의 중심은 통계가 아니라 <b>오늘 할 일 하나</b>다 (tenant-plan.md §2.1).
 * 그래서 헤드라인 문장을 만드는 두 값({@code todayConvCount}, {@code openGapCount})이 먼저 온다.
 *
 * @param avgResponseMs 잰 적이 없으면 null. 0 으로 채우지 않는다 — "0ms 에 답했다"는 거짓이다
 */
public record HomeSummaryResponse(
        long todayConvCount,
        long todayConvDelta,
        Integer answerSuccessPercent,
        Integer answerSuccessPercentLastWeek,
        long openGapCount,
        long todayLeadCount,
        long weekLeadCount,
        Integer avgResponseMs,
        Knowledge knowledge,
        List<TopQuestion> topQuestions) {

    /**
     * 지식 상태 막대. 문서 수백 개를 스크롤 없이 한눈에 보여주기 위한 값이다.
     *
     * <p>{@code documentCount} 는 업체가 제외(EXCLUDED)한 문서를 빼고 센다 —
     * 요금제 한도와 같은 기준이어야 요금제 화면과 숫자가 어긋나지 않는다.
     */
    public record Knowledge(long documentCount, long indexedCount, long processingCount, long failedCount) {
    }

    public record TopQuestion(String question, long askCount) {
    }
}
