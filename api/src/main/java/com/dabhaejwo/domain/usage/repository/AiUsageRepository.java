package com.dabhaejwo.domain.usage.repository;

import com.dabhaejwo.domain.usage.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
}
