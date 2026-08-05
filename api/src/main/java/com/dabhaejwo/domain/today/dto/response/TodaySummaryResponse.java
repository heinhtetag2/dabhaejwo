package com.dabhaejwo.domain.today.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 오늘 화면. api-contracts.md §5.
 *
 * <p>지표를 나열하면 무엇이 중요한지 사라진다. 최상단에는 <b>문장 하나</b>만 크게 두고
 * 나머지는 그 아래로 내린다 (admin-console-plan.md §2.2).
 */
public record TodaySummaryResponse(
        Headline headline,
        Stats stats,
        List<Action> actions,
        System system,
        /** 당일 집계가 몇 시 기준인지. 한 번도 집계된 적 없으면 null. */
        OffsetDateTime aggregatedAt) {

    public record Headline(long tenantCount, long costExceededCount) {
    }

    public record Stats(
            long payingTenantCount,
            long mrrKrw,
            long todayConvCount,
            BigDecimal todayCostKrw) {
    }

    /**
     * 조치 항목. {@code targetPath} 로 <b>바로 그 대상</b>에 도달해야 한다 —
     * 이름만 확인하고 다시 검색하게 만들지 않는다.
     */
    public record Action(
            Type type,
            UUID tenantId,
            String title,
            String detail,
            String targetPath) {

        public enum Type {
            COST_EXCEEDED,
            JOB_FAILED,
            PAYMENT_FAILED,
            TRIAL_ENDING,
            TICKET_WAITING
        }
    }

    /**
     * 시스템 상태.
     *
     * <p><b>측정 지점이 없는 값은 {@code null} 이다.</b> 답변 파이프라인·크롤러 워커·APM 이
     * 아직 없어 응답 시간·워커 가동·벡터 DB 사용량·5xx 건수는 잴 곳이 없다. 0 으로 채우면
     * "응답 0ms, 오류 0건"이라는 거짓이 되고 운영자는 정상이라고 읽는다.
     *
     * <p>{@code embedQueueDepth} 만 실값이다 — {@code jobs} 테이블에서 실제로 센다(현재 0).
     */
    public record System(
            Integer chatApiP95Ms,
            long embedQueueDepth,
            String crawlerWorkers,
            Integer vectorDbUsagePercent,
            Integer todayError5xxCount,
            List<RecentError> recentErrors) {

        public record RecentError(OffsetDateTime at, String tenantName, String code) {
        }
    }
}
