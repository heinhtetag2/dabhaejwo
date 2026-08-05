package com.dabhaejwo.domain.billing.repository;

import com.dabhaejwo.domain.billing.entity.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {

    List<BillingRecord> findAllByTenantIdOrderByPeriodDesc(UUID tenantId);

    /**
     * <b>가장 최근</b> 결제 시도가 실패한 업체들.
     *
     * <p>"실패 기록이 하나라도 있는 업체"가 아니다 — 지난달 실패하고 이번 달 정상 결제된
     * 업체를 계속 결제 실패로 세면 필터가 쓸모없어진다.
     */
    @Query("""
            SELECT DISTINCT b.tenantId FROM BillingRecord b
            WHERE b.status = com.dabhaejwo.domain.billing.entity.BillingStatus.FAILED
              AND b.period = (SELECT MAX(latest.period) FROM BillingRecord latest
                              WHERE latest.tenantId = b.tenantId)
            """)
    List<UUID> findTenantIdsWithLatestPaymentFailed();
}
