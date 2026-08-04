package com.dabhaejwo.domain.billing.repository;

import com.dabhaejwo.domain.billing.entity.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {

    List<BillingRecord> findAllByTenantIdOrderByPeriodDesc(UUID tenantId);
}
