package com.dabhaejwo.domain.knowledge.dto.response;

import com.dabhaejwo.domain.knowledge.entity.KnowledgeSource;
import com.dabhaejwo.domain.knowledge.entity.SourceType;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 지식 소스. api-contracts.md §9-2. */
public record KnowledgeSourceResponse(
        UUID id,
        SourceType type,
        String origin,
        boolean autoRefresh,
        OffsetDateTime lastCrawledAt,
        long documentCount) {

    public static KnowledgeSourceResponse of(KnowledgeSource source, long documentCount) {
        return new KnowledgeSourceResponse(
                source.getId(),
                source.getType(),
                source.getOrigin(),
                source.isAutoRefresh(),
                source.getLastCrawledAt(),
                documentCount);
    }
}
