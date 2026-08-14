package com.dabhaejwo.domain.knowledge.entity;

import com.dabhaejwo.global.security.BotScope;
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

    /** 어느 서비스의 것인가. 조회는 전부 이 값으로 좁힌다. */
    @Column(name = "bot_id", nullable = false)
    private UUID botId;

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

    public static KnowledgeSource of(BotScope scope, SourceType type, String origin, boolean autoRefresh) {
        KnowledgeSource source = new KnowledgeSource();
        source.tenantId = scope.tenantId();
        source.botId = scope.botId();
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

    public UUID getBotId() {
        return botId;
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
