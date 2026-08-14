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
        /**
         * 서비스별 이번 달 사용량.
         *
         * <p>한도는 업체 합산이라 <b>"왜 한도가 찼는지"를 업체가 스스로 볼 방법이 없었다.</b>
         * 서비스가 하나면 합계와 같으므로 화면은 이 목록을 그리지 않는다.
         *
         * <p>합이 {@code usage.convCount} 보다 작을 수 있다 — 서비스 구분 이전(V16 이전)
         * 호출은 어느 서비스 것인지 복원할 수 없다. 그 차이를 지어내 메우지 않는다.
         */
        List<BotUsage> botUsage,
        List<BillingItem> billingRecords) {

    public record BotUsage(UUID botId, String botName, long convCount, long docCount) {
    }

    public record Plan(UUID id, String name, int monthlyFee) {
    }

    /** 필드명은 §2 TenantDetail 과 완전히 같다. 부분집합일 뿐이다. */
    public record Usage(long convCount, int convLimit, long docCount, int docLimit) {
    }

    /**
     * @param receiptUrl 토스가 준 영수증 주소. 결제 전·실패 행은 null 이다
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
