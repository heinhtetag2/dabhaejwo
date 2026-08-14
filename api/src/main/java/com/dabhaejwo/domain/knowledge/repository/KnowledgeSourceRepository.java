package com.dabhaejwo.domain.knowledge.repository;

import com.dabhaejwo.domain.knowledge.entity.KnowledgeSource;
import com.dabhaejwo.domain.knowledge.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, UUID> {

    List<KnowledgeSource> findAllByBotIdOrderByCreatedAtAsc(UUID botId);

    Optional<KnowledgeSource> findByIdAndBotId(UUID id, UUID botId);

    Optional<KnowledgeSource> findFirstByBotIdAndType(UUID botId, SourceType type);
}
