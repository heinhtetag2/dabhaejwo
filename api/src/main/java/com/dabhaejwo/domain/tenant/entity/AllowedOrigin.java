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

    /**
     * 저장 형태는 호스트명이다 — 스킴·포트·경로를 붙이지 않는다.
     * 위젯이 보내는 {@code Origin} 헤더에서 호스트만 떼어 비교하므로 형태가 어긋나면 전부 403 이 된다.
     */
    public static AllowedOrigin of(UUID tenantId, String origin) {
        AllowedOrigin allowed = new AllowedOrigin();
        allowed.tenantId = tenantId;
        allowed.origin = normalizeHost(origin);
        allowed.createdAt = OffsetDateTime.now();
        return allowed;
    }

    /**
     * 스킴·포트·경로를 떼고 소문자 호스트만 남긴다.
     * 저장 형태와 비교 형태가 어긋나면 위젯 호출이 전부 403 이 되므로 한 곳에서만 만든다.
     */
    public static String normalizeHost(String raw) {
        String value = raw == null ? "" : raw.strip().toLowerCase(java.util.Locale.ROOT);
        value = value.replaceFirst("^https?://", "");
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(0, colon);
        }
        return value;
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
