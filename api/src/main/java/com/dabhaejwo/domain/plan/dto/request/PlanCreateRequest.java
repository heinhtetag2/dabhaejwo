package com.dabhaejwo.domain.plan.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 새 요금제. {@code code} 는 만든 뒤 바꿀 수 없다 — 코드가 이 값으로 요금제를 찾는다.
 *
 * <p>협의가는 {@code monthlyFee = 0} + {@code negotiable = true} 로 표현한다.
 * 둘을 따로 두는 이유는 "0원 무료"와 "가격 미정"이 다른 상태이기 때문이다.
 */
public record PlanCreateRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]*",
                message = "코드는 대문자·숫자·밑줄만 쓸 수 있습니다") String code,
        @NotBlank String name,
        @Min(0) int monthlyFee,
        boolean negotiable,
        @Min(0) int convLimit,
        @Min(0) int docLimit,
        int sortOrder) {
}
