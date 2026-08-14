package com.dabhaejwo.domain.tenant.entity;

import com.dabhaejwo.global.common.HostName;
import com.dabhaejwo.global.security.BotScope;
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

    /** 어느 서비스의 주소인가. 위젯 인증이 이 값으로 좁힌다. */
    @Column(name = "bot_id", nullable = false)
    private UUID botId;

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
    public static AllowedOrigin of(BotScope scope, String origin) {
        AllowedOrigin allowed = new AllowedOrigin();
        allowed.tenantId = scope.tenantId();
        allowed.botId = scope.botId();
        allowed.origin = normalizeHost(origin);
        allowed.createdAt = OffsetDateTime.now();
        return allowed;
    }

    /**
     * 스킴·포트·경로를 떼고 소문자 호스트만 남긴다.
     *
     * <p>규칙 자체는 {@link HostName} 이 갖는다 — 서비스 대표 도메인도 같은 규칙을 써야 하는데
     * 두 도메인이 각자 구현하면 언젠가 갈리고, 갈리는 순간 위젯 호출이 전부 403 이 된다.
     */
    public static String normalizeHost(String raw) {
        return HostName.normalize(raw);
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

    public UUID getBotId() {
        return botId;
    }

    public String getOrigin() {
        return origin;
    }

    public OffsetDateTime getLastCalledAt() {
        return lastCalledAt;
    }
}
