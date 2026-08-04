package com.dabhaejwo.domain.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 지식 소스 — 웹사이트 한 곳, 업로드 파일 묶음, 직접 입력 묶음. */
@Entity
@Table(name = "knowledge_sources")
public class KnowledgeSource {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    /** 웹사이트 도메인, 또는 파일·직접입력 묶음의 표시명. */
    @Column(nullable = false)
    private String origin;

    @Column(name = "auto_refresh", nullable = false)
    private boolean autoRefresh;

    @Column(name = "last_crawled_at")
    private OffsetDateTime lastCrawledAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected KnowledgeSource() {
    }

    public static KnowledgeSource of(UUID tenantId, SourceType type, String origin, boolean autoRefresh) {
        KnowledgeSource source = new KnowledgeSource();
        source.tenantId = tenantId;
        source.type = type;
        source.origin = origin;
        source.autoRefresh = autoRefresh;
        source.createdAt = OffsetDateTime.now();
        return source;
    }

    public void changeAutoRefresh(boolean value) {
        this.autoRefresh = value;
    }

    public void markCrawled() {
        this.lastCrawledAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public SourceType getType() {
        return type;
    }

    public String getOrigin() {
        return origin;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public OffsetDateTime getLastCrawledAt() {
        return lastCrawledAt;
    }
}
