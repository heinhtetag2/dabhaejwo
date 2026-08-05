package com.dabhaejwo.domain.plan.dto.request;

import com.dabhaejwo.global.llm.LlmProviderName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * 요금제별 모델 배정 저장.
 *
 * <p>조각 수 상한을 두는 이유는 입력 토큰이 조각 수에 비례하기 때문이다 —
 * 실수로 100을 넣으면 대화당 원가가 10배가 되고, 그 사실은 다음 달 정산에서야 드러난다.
 * 권장 범위는 5~10 이다 (admin-console-plan.md §4.7).
 */
public record PlanModelAssignmentRequest(
        @NotNull UUID planId,
        @NotNull LlmProviderName provider,
        @NotBlank String model,
        @Min(1) @Max(20) int chunkCount) {
}
