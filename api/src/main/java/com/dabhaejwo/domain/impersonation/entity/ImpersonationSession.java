package com.dabhaejwo.domain.impersonation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 운영자가 업체 대시보드에 대리 접속한 세션.
 *
 * <p>사유 없이는 만들어지지 않고(DB CHECK + 서비스 검증), 업체 대시보드의 계정 화면에
 * 시각과 사유가 그대로 공개된다. 숨기는 편이 편하지만 공개하는 쪽이 신뢰 확보에 유리하며
 * 개인정보 처리방침 고지 의무에도 부합한다 (tenant-plan.md §6.3).
 */
@Entity
@Table(name = "impersonation_sessions")
public class ImpersonationSession {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImpersonationStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    protected ImpersonationSession() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public String getReason() {
        return reason;
    }

    public ImpersonationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    /** 만료 시각이 지났으면 상태와 무관하게 끝난 세션이다. 배치가 늦어도 화면은 정확해야 한다. */
    public boolean active(OffsetDateTime now) {
        return status == ImpersonationStatus.ACTIVE && expiresAt.isAfter(now);
    }
}
