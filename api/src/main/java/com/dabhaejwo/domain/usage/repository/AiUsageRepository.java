package com.dabhaejwo.domain.usage.repository;

import com.dabhaejwo.domain.usage.entity.AiUsage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    /**
     * 업체 상세 화면 전용 실시간 집계. 단일 테넌트라 허용된다.
     * 목록·대시보드는 tenant_daily_usage 를 읽는다 (admin-console-plan.md §6.1).
     */
    @Query("""
            SELECT COALESCE(SUM(u.costKrw), 0)
            FROM AiUsage u
            WHERE u.tenantId = :tenantId
              AND u.createdAt >= :from
              AND u.createdAt < :to
            """)
    BigDecimal sumCostKrw(@Param("tenantId") UUID tenantId,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to);

    /**
     * 전 업체 합계. 전체 일일 원가 상한 판정이 쓴다 — 업체별만 보면
     * 작은 업체 여럿이 동시에 한도까지 쓰는 상황을 못 막는다.
     */
    @Query("""
            SELECT COALESCE(SUM(u.costKrw), 0)
            FROM AiUsage u
            WHERE u.createdAt >= :from
              AND u.createdAt < :to
            """)
    BigDecimal sumCostKrwAll(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /** 기간 전체 합계. AI 사용량 화면의 지표 4종이 쓴다. */
    @Query("""
            SELECT COALESCE(SUM(u.inputTokens), 0) AS tokensIn,
                   COALESCE(SUM(u.outputTokens), 0) AS tokensOut,
                   COALESCE(SUM(u.costKrw), 0) AS costKrw,
                   COUNT(u) AS callCount
            FROM AiUsage u
            WHERE u.createdAt >= :from AND u.createdAt < :to
            """)
    Totals totalsBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /**
     * 모델 × 용도별 집계. 용도를 나누지 않고 모델별로만 집계하면 같은 모델이 답변용인지
     * 요약용인지 구분되지 않아 절감 지점을 찾을 수 없다 (admin-console-plan.md §4.4).
     */
    @Query("""
            SELECT u.provider AS provider, u.model AS model, u.purpose AS purpose,
                   COUNT(u) AS callCount,
                   COALESCE(SUM(u.inputTokens), 0) AS inputTokens,
                   COALESCE(SUM(u.outputTokens), 0) AS outputTokens,
                   COALESCE(SUM(u.costKrw), 0) AS costKrw
            FROM AiUsage u
            WHERE u.createdAt >= :from AND u.createdAt < :to
            GROUP BY u.provider, u.model, u.purpose
            ORDER BY SUM(u.costKrw) DESC
            """)
    List<ModelUsage> aggregateByModel(@Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to);

    /**
     * 날짜 × 용도별 원가. 14일 누적 막대가 쓴다.
     *
     * <p>네이티브 쿼리인 이유는 {@code created_at} 을 날짜로 잘라 묶어야 하는데
     * 그 캐스팅이 방언마다 다르기 때문이다. 이 프로젝트는 PostgreSQL 고정이다.
     */
    @Query(value = """
            SELECT (created_at AT TIME ZONE 'UTC')::date AS day,
                   purpose AS purpose,
                   COALESCE(SUM(cost_krw), 0) AS costKrw
            FROM ai_usage
            WHERE created_at >= :from AND created_at < :to
            GROUP BY 1, 2
            ORDER BY 1
            """, nativeQuery = true)
    List<DailyPurposeCost> aggregateDailyByPurpose(@Param("from") OffsetDateTime from,
                                                   @Param("to") OffsetDateTime to);

    /** 원가 상위 업체. */
    @Query("""
            SELECT u.tenantId AS tenantId,
                   COALESCE(SUM(u.inputTokens + u.outputTokens), 0) AS tokens,
                   COALESCE(SUM(u.costKrw), 0) AS costKrw
            FROM AiUsage u
            WHERE u.createdAt >= :from AND u.createdAt < :to
            GROUP BY u.tenantId
            ORDER BY SUM(u.costKrw) DESC
            """)
    List<TenantCost> aggregateByTenant(@Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to,
                                       Pageable pageable);

    /**
     * 월별 원가. 정산 화면의 월별 추이가 쓴다.
     *
     * <p>하루를 <b>UTC</b> 로 끊는다 — AI 사용량 화면(§6)과 같은 기준이어야 하기 때문이다.
     * 같은 "이번 달 모델 원가"가 두 화면에서 다르게 나오면 어느 쪽도 믿을 수 없다.
     * 청구·가입은 {@code BusinessDay}(KST) 기준이라 두 기준이 섞여 있는데, 이는
     * 이미 등록된 한계다 ({@code docs/IMPROVEMENTS.md} — 집계 테이블의 하루 경계).
     * 월 단위라 경계에 걸리는 금액은 작다.
     */
    @Query(value = """
            SELECT date_trunc('month', created_at AT TIME ZONE 'UTC')::date AS month,
                   COALESCE(SUM(cost_krw), 0) AS costKrw
            FROM ai_usage
            WHERE created_at >= :from AND created_at < :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<MonthlyCost> aggregateMonthlyCost(@Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to);

    /**
     * 지정한 업체들의 원가 합.
     *
     * <p>체험 업체의 원가를 따로 세는 데 쓴다 — 매출이 0원이라 <b>전액이 손실</b>인데
     * 원가율(정가 나누기)로는 드러나지 않는다.
     *
     * <p>빈 목록으로 부르지 않는다. JPQL 의 {@code IN ()} 은 문법 오류다.
     */
    @Query("""
            SELECT COALESCE(SUM(u.costKrw), 0)
            FROM AiUsage u
            WHERE u.createdAt >= :from AND u.createdAt < :to
              AND u.tenantId IN :tenantIds
            """)
    BigDecimal sumCostKrwByTenants(@Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to,
                                   @Param("tenantIds") List<UUID> tenantIds);

    /** 일 집계 배치가 읽는다. 하루치를 테넌트별로 묶는다. */
    @Query("""
            SELECT u.tenantId AS tenantId,
                   COALESCE(SUM(u.inputTokens), 0) AS tokensIn,
                   COALESCE(SUM(u.outputTokens), 0) AS tokensOut,
                   COALESCE(SUM(u.costKrw), 0) AS costKrw
            FROM AiUsage u
            WHERE u.createdAt >= :from AND u.createdAt < :to
            GROUP BY u.tenantId
            """)
    List<TenantDayTotal> aggregateForDay(@Param("from") OffsetDateTime from,
                                         @Param("to") OffsetDateTime to);

    /**
     * 같은 하루치를 <b>서비스별로</b> 묶는다. 업체 축 쿼리는 건드리지 않는다 —
     * 운영 콘솔 전 화면이 그 숫자를 읽는다.
     *
     * <p>{@code bot_id} 가 null 인 행(서비스 구분 이전 호출)은 빠진다. 그래서
     * <b>서비스별 합계가 업체 합계보다 작을 수 있다</b> — 화면이 그 차이를 지어내 메우지 않는다.
     */
    @Query("""
            SELECT u.botId AS botId,
                   COALESCE(SUM(u.inputTokens), 0) AS tokensIn,
                   COALESCE(SUM(u.outputTokens), 0) AS tokensOut,
                   COALESCE(SUM(u.costKrw), 0) AS costKrw
            FROM AiUsage u
            WHERE u.createdAt >= :from AND u.createdAt < :to AND u.botId IS NOT NULL
            GROUP BY u.botId
            """)
    List<BotDayTotal> aggregateForDayByBot(@Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to);

    interface BotDayTotal {
        java.util.UUID getBotId();

        long getTokensIn();

        long getTokensOut();

        java.math.BigDecimal getCostKrw();
    }

    interface Totals {
        long getTokensIn();

        long getTokensOut();

        BigDecimal getCostKrw();

        long getCallCount();
    }

    interface ModelUsage {
        String getProvider();

        String getModel();

        String getPurpose();

        long getCallCount();

        long getInputTokens();

        long getOutputTokens();

        BigDecimal getCostKrw();
    }

    interface DailyPurposeCost {
        LocalDate getDay();

        String getPurpose();

        BigDecimal getCostKrw();
    }

    interface MonthlyCost {
        LocalDate getMonth();

        BigDecimal getCostKrw();
    }

    interface TenantCost {
        UUID getTenantId();

        long getTokens();

        BigDecimal getCostKrw();
    }

    interface TenantDayTotal {
        UUID getTenantId();

        long getTokensIn();

        long getTokensOut();

        BigDecimal getCostKrw();
    }
}
