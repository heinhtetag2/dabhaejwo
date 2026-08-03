package com.dabhaejwo.global.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 감사 기록. <b>수정·삭제 불가, 3년 보존.</b>
 *
 * <p>수정자가 없는 것에 더해 DB 트리거로도 UPDATE/DELETE 를 막아 두었다(V1__init.sql).
 * 앱 레이어가 실수하거나 뚫려도 기록은 못 고친다.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    static AuditLog of(UUID operatorId,
                       AuditAction action,
                       UUID tenantId,
                       String reason,
                       Map<String, Object> meta) {
        AuditLog entry = new AuditLog();
        entry.operatorId = operatorId;
        entry.action = action;
        entry.tenantId = tenantId;
        entry.reason = reason;
        entry.meta = meta == null ? Map.of() : meta;
        entry.createdAt = OffsetDateTime.now();
        return entry;
    }

    public Long getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
