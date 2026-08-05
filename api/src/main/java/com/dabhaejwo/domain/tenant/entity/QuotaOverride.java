package com.dabhaejwo.domain.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 이번 달 한정 쿼터 증량.
 *
 * <p>요금제의 한도를 고치지 않고 델타만 쌓는다. 다음 달이 되면 {@code period} 가 달라져
 * 자동으로 원복되고 이력만 남는다 (admin-console-tenant-plan.md §9).
 *
 * <p>수정자가 없다. 잘못 넣었으면 반대 부호로 한 건 더 넣는다 — 증량 이력은 영업·CS 근거다.
 */
@Entity
@Table(name = "quota_overrides")
public class QuotaOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 적용 월의 1일. */
    @Column(nullable = false)
    private LocalDate period;

    @Column(name = "conv_delta", nullable = false)
    private int convDelta;

    @Column(name = "doc_delta", nullable = false)
    private int docDelta;

    @Column(nullable = false)
    private String reason;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected QuotaOverride() {
    }

    public static QuotaOverride grant(UUID tenantId,
                                      LocalDate period,
                                      int convDelta,
                                      int docDelta,
                                      String reason,
                                      UUID operatorId) {
        QuotaOverride override = new QuotaOverride();
        override.tenantId = tenantId;
        override.period = period.withDayOfMonth(1);
        override.convDelta = convDelta;
        override.docDelta = docDelta;
        override.reason = reason;
        override.operatorId = operatorId;
        override.createdAt = OffsetDateTime.now();
        return override;
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public int getConvDelta() {
        return convDelta;
    }

    public int getDocDelta() {
        return docDelta;
    }

    public String getReason() {
        return reason;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
