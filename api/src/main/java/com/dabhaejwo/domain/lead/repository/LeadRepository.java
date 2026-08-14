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

    Page<Lead> findAllByBotIdOrderByCreatedAtDesc(UUID botId, Pageable pageable);

    List<Lead> findAllByBotIdOrderByCreatedAtDesc(UUID botId);

    Optional<Lead> findByIdAndBotId(UUID id, UUID botId);

    long countByBotIdAndCreatedAtBetween(UUID botId, OffsetDateTime from, OffsetDateTime to);

    long countByBotIdAndStatus(UUID botId, LeadStatus status);
}
