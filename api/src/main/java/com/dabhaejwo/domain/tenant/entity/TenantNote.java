package com.dabhaejwo.domain.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체 내부 메모. <b>누적이다.</b>
 *
 * <p>수정·삭제 메서드가 없고 엔드포인트도 없다. 영업 이력과 CS 대응 맥락이 담기므로
 * 덮어쓰면 "왜 이렇게 대응했는지"가 사라진다 (admin-console-tenant-plan.md §4.2.4).
 */
@Entity
@Table(name = "tenant_notes")
public class TenantNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String body;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TenantNote() {
    }

    public static TenantNote write(UUID tenantId, String body, UUID operatorId) {
        TenantNote note = new TenantNote();
        note.tenantId = tenantId;
        note.body = body;
        note.operatorId = operatorId;
        note.createdAt = OffsetDateTime.now();
        return note;
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getBody() {
        return body;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
