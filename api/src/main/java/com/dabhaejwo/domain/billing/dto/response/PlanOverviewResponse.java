package com.dabhaejwo.domain.billing.dto.response;

import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.entity.BillingStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 요금제 화면. api-contracts.md §9-3 의 {@code GET /api/app/plan}.
 *
 * @param savedAnswerPercent 저장 답변으로 처리돼 한도를 쓰지 않은 비율.
 *                           "이번 달 대화의 N%는 한도를 쓰지 않았습니다"로 공통 질문 등록을 유도한다
 *                           (tenant-plan.md §4.9)
 */
public record PlanOverviewResponse(
        Plan plan,
        Usage usage,
        LocalDate nextBillingDate,
        Integer savedAnswerPercent,
        List<BillingItem> billingRecords) {

    public record Plan(UUID id, String name, int monthlyFee) {
    }

    /** 필드명은 §2 TenantDetail 과 완전히 같다. 부분집합일 뿐이다. */
    public record Usage(long convCount, int convLimit, long docCount, int docLimit) {
    }

    /**
     * @param receiptUrl TODO(stub): PG 미연동이라 항상 null 이다. 화면은 "준비 중"으로 표시한다
     */
    public record BillingItem(
            Long id,
            LocalDate period,
            int amount,
            BillingStatus status,
            String failureReason,
            String receiptUrl) {

        static BillingItem from(BillingRecord record) {
            return new BillingItem(
                    record.getId(),
                    record.getPeriod(),
                    record.getAmount(),
                    record.getStatus(),
                    record.getFailureReason(),
                    record.getReceiptUrl());
        }
    }

    public static List<BillingItem> toItems(List<BillingRecord> records) {
        return records.stream().map(BillingItem::from).toList();
    }
}
