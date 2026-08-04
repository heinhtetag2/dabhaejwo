package com.dabhaejwo.domain.plan.dto.response;

import com.dabhaejwo.domain.plan.entity.Plan;

import java.util.UUID;

/**
 * 공개 요금제. 로그인 없이 읽힌다.
 *
 * <p>운영 콘솔의 요금제 응답(api-contracts.md §7)과 <b>같은 리소스</b>이며 필드를 빼기만 했다.
 * {@code tenantCount}(사용 업체 수)는 대외 공개 정보가 아니다 — 같은 뜻의 키를 다른 이름으로
 * 만들지 않고 그냥 내보내지 않는다.
 *
 * @param negotiable 협의가(기업 요금제). true 면 화면이 금액 대신 "문의"를 보여준다
 */
public record PublicPlanResponse(
        UUID id,
        String code,
        String name,
        int monthlyFee,
        boolean negotiable,
        int convLimit,
        int docLimit) {

    public static PublicPlanResponse from(Plan plan) {
        return new PublicPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getMonthlyFee(),
                plan.isNegotiable(),
                plan.getConvLimit(),
                plan.getDocLimit());
    }
}
