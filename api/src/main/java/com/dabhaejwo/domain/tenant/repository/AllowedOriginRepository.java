package com.dabhaejwo.domain.tenant.repository;

import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AllowedOriginRepository extends JpaRepository<AllowedOrigin, UUID> {

    List<AllowedOrigin> findAllByTenantId(UUID tenantId);
}
