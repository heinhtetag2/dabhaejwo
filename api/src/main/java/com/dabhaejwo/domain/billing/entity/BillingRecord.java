package com.dabhaejwo.domain.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 청구 이력 한 건. 업체 요금제 화면의 "결제 내역"이다. */
@Entity
@Table(name = "billing_records")
public class BillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 청구 대상 월의 1일. */
    @Column(nullable = false)
    private LocalDate period;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "failure_reason")
    private String failureReason;

    /** TODO(stub): PG 미연동이라 항상 null 이다. 결제 붙일 때 채운다. */
    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BillingRecord() {
    }

    public static BillingRecord of(UUID tenantId, LocalDate period, int amount, BillingStatus status) {
        BillingRecord record = new BillingRecord();
        record.tenantId = tenantId;
        record.period = period;
        record.amount = amount;
        record.status = status;
        record.attempts = status == BillingStatus.PENDING ? 0 : 1;
        record.createdAt = OffsetDateTime.now();
        return record;
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public int getAttempts() {
        return attempts;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public int getAmount() {
        return amount;
    }

    public BillingStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }
}
