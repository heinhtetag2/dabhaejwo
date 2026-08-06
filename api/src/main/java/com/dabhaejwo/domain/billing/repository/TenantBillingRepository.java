package com.dabhaejwo.domain.billing.repository;

import com.dabhaejwo.domain.billing.entity.TenantBilling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantBillingRepository extends JpaRepository<TenantBilling, UUID> {
}
