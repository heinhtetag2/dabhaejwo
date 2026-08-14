package com.dabhaejwo.domain.plan.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 요금제 수정. {@code code} 가 없는 것이 설계다 — 바꿀 수 없는 값이다.
 *
 * <p>{@code sellable} 이 삭제를 대신한다. 판매를 멈춰도 기존 계약 업체는 그대로 남는다.
 */
public record PlanUpdateRequest(
        @NotBlank String name,
        @Min(0) int monthlyFee,
        boolean negotiable,
        @Min(0) int convLimit,
        @Min(0) int docLimit,
        /** 만들 수 있는 서비스 수. 생성 시점에만 검사한다. */
        @Min(1) int botLimit,
        boolean sellable) {
}
