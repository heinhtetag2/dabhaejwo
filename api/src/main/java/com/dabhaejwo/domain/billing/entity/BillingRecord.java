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

    /** 주문번호. 업체 + 청구월로 결정되므로 재시도해도 같은 값이다 — 중복 청구를 막는다. */
    @Column(name = "order_id")
    private String orderId;

    /** 토스 쪽 결제 식별자. 분쟁이 생겼을 때 대조할 유일한 값이다. */
    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    private String method;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BillingRecord() {
    }

    /**
     * 청구 성공. <b>주문번호와 결제 식별자를 함께 남긴다</b> —
     * 분쟁이 생겼을 때 토스 쪽 기록과 대조할 유일한 값이다.
     */
    public void markPaid(String orderId, String paymentKey, String receiptUrl, String method) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.receiptUrl = receiptUrl;
        this.method = method;
        this.status = BillingStatus.PAID;
        this.paidAt = OffsetDateTime.now();
        this.failureReason = null;
    }

    /**
     * 청구 실패. 시도 횟수를 올리고 사유를 남긴다.
     *
     * <p>실패를 기록하지 않으면 "왜 못 받았는지"를 알 방법이 없다. 카드사 문구를 그대로
     * 담는 이유도 같다 — "한도 초과"는 업체가 바로 조치할 수 있는 정보다.
     */
    public void markFailed(String orderId, String reason) {
        this.orderId = orderId;
        this.status = BillingStatus.FAILED;
        this.attempts = this.attempts + 1;
        this.failureReason = reason;
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

    public String getOrderId() {
        return orderId;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public String getMethod() {
        return method;
    }
}
