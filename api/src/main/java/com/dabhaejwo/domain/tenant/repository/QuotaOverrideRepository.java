package com.dabhaejwo.domain.tenant.repository;

import com.dabhaejwo.domain.tenant.entity.QuotaOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface QuotaOverrideRepository extends JpaRepository<QuotaOverride, Long> {

    List<QuotaOverride> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * 이번 달 증량분 합계. 여러 번 증량했을 수 있으므로 더한다.
     * 없으면 0 이다 — null 이 아니다.
     */
    @Query("""
            SELECT COALESCE(SUM(q.convDelta), 0) AS convDelta,
                   COALESCE(SUM(q.docDelta), 0)  AS docDelta
            FROM QuotaOverride q
            WHERE q.tenantId = :tenantId AND q.period = :period
            """)
    Delta sumDelta(@Param("tenantId") UUID tenantId, @Param("period") LocalDate period);

    interface Delta {
        int getConvDelta();

        int getDocDelta();
    }
}
