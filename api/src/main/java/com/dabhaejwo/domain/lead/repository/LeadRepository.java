package com.dabhaejwo.domain.lead.repository;

import com.dabhaejwo.domain.lead.entity.Lead;
import com.dabhaejwo.domain.lead.entity.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<Lead> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantIdAndCreatedAtBetween(UUID tenantId, OffsetDateTime from, OffsetDateTime to);

    long countByTenantIdAndStatus(UUID tenantId, LeadStatus status);
}
