package com.dabhaejwo.domain.tenant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 체험 연장. 상한을 두는 이유는 실수로 큰 수를 넣으면 사실상 영구 무료 계정이 되고,
 * 그 상태가 요금제 화면 어디에도 드러나지 않기 때문이다.
 */
public record TrialExtensionRequest(
        @Min(1) @Max(90) int days,
        String reason) {
}
