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

    /** 프로젝션. 필드명은 위 쿼리의 alias 와 일치해야 한다. */
    interface MonthlyTotal {
        UUID getTenantId();

        long getConvCount();

        long getSavedCount();

        java.math.BigDecimal getCostKrw();
    }
}
