package com.dabhaejwo.domain.billing.dto.response;

import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.entity.BillingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 청구 한 건 (api-contracts.md §6-1).
 *
 * <p>{@code paymentKey} 를 싣는 이유는 분쟁이 생겼을 때 토스 쪽 기록과 대조할 유일한
 * 값이기 때문이다. <b>결제를 실행할 수 있는 값이 아니다</b> — 실행에 필요한 빌링키는
 * 어떤 응답에도 나가지 않는다.
 */
public record BillingRecordResponse(
        Long id,
        TenantRef tenant,
        String planName,
        String period,
        int amountKrw,
        BillingStatus status,
        int attempts,
        String failureReason,
        String orderId,
        String paymentKey,
        OffsetDateTime paidAt,
        String method,
        String receiptUrl) {

    public record TenantRef(UUID id, String name) {
    }

    public static BillingRecordResponse of(BillingRecord record, String tenantName, String planName) {
        return new BillingRecordResponse(
                record.getId(),
                new TenantRef(record.getTenantId(), tenantName),
                planName,
                // 청구월은 그 달 1일로 저장된다. 화면·계약은 YYYY-MM 이다.
                record.getPeriod().toString().substring(0, 7),
                record.getAmount(),
                record.getStatus(),
                record.getAttempts(),
                record.getFailureReason(),
                record.getOrderId(),
                record.getPaymentKey(),
                record.getPaidAt(),
                record.getMethod(),
                record.getReceiptUrl());
    }
}
