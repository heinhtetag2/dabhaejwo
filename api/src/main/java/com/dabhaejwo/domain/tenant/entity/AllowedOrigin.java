package com.dabhaejwo.domain.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 위젯이 호출할 수 있는 Origin.
 *
 * <p>공개 키({@code pk_live_*})는 남의 사이트 소스에 그대로 노출된다.
 * 그래도 안전한 이유가 이 화이트리스트다 — 등록되지 않은 도메인에서는 동작하지 않는다.
 */
@Entity
@Table(name = "allowed_origins")
public class AllowedOrigin {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String origin;

    @Column(name = "last_called_at")
    private OffsetDateTime lastCalledAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AllowedOrigin() {
    }

    public void markCalled() {
        this.lastCalledAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getOrigin() {
        return origin;
    }

    public OffsetDateTime getLastCalledAt() {
        return lastCalledAt;
    }
}
