package com.dabhaejwo.domain.usage.repository;

import com.dabhaejwo.domain.usage.entity.TenantDailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TenantDailyUsageRepository
        extends JpaRepository<TenantDailyUsage, TenantDailyUsage.Key> {

    /**
     * 기간 내 업체별 합계. 업체 목록 화면이 N+1 없이 원가율을 계산하려면
     * 한 번의 질의로 전 업체 집계를 받아야 한다.
     */
    @Query("""
            SELECT u.tenantId AS tenantId,
                   COALESCE(SUM(u.convCount), 0) AS convCount,
                   COALESCE(SUM(u.savedCount), 0) AS savedCount,
                   COALESCE(SUM(u.costKrw), 0) AS costKrw
            FROM TenantDailyUsage u
            WHERE u.day >= :from AND u.day <= :to
            GROUP BY u.tenantId
            """)
    List<MonthlyTotal> aggregateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * 단일 업체 합계. 업체 대시보드는 자기 테넌트 하나만 보므로 전체 집계를 돌릴 이유가 없다.
     * 결과가 없으면(그 달에 대화가 없으면) 0 을 반환한다 — null 이 아니다.
     */
    @Query("""
            SELECT COALESCE(SUM(u.convCount), 0) FROM TenantDailyUsage u
            WHERE u.tenantId = :tenantId AND u.day >= :from AND u.day <= :to
            """)
    long sumConvCount(@Param("tenantId") UUID tenantId,
                      @Param("from") LocalDate from,
                      @Param("to") LocalDate to);

    /** 프로젝션. 필드명은 위 쿼리의 alias 와 일치해야 한다. */
    interface MonthlyTotal {
        UUID getTenantId();

        long getConvCount();

        long getSavedCount();

        java.math.BigDecimal getCostKrw();
    }
}
