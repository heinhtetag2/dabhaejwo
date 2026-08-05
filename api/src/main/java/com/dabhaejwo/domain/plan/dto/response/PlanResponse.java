package com.dabhaejwo.domain.plan.dto.response;

import com.dabhaejwo.domain.plan.entity.Plan;

import java.util.UUID;

/**
 * 요금제. api-contracts.md §7.
 *
 * <p>{@code tenantCount} 를 함께 주는 이유는 판매 중단 판단에 필요하기 때문이다 —
 * 쓰는 업체가 남아 있는데 지우려 드는 일을 막는다. 요금제에 <b>삭제는 없다.</b>
 */
public record PlanResponse(
        UUID id,
        String code,
        String name,
        int monthlyFee,
        boolean negotiable,
        int convLimit,
        int docLimit,
        long tenantCount,
        boolean sellable) {

    public static PlanResponse of(Plan plan, long tenantCount) {
        return new PlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getMonthlyFee(),
                plan.isNegotiable(),
                plan.getConvLimit(),
                plan.getDocLimit(),
                tenantCount,
                plan.isSellable());
    }
}
