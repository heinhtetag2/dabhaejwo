package com.dabhaejwo.domain.knowledge.repository;

import com.dabhaejwo.domain.knowledge.entity.KnowledgeSource;
import com.dabhaejwo.domain.knowledge.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, UUID> {

    List<KnowledgeSource> findAllByTenantIdOrderByCreatedAtAsc(UUID tenantId);

    Optional<KnowledgeSource> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<KnowledgeSource> findFirstByTenantIdAndType(UUID tenantId, SourceType type);
}
