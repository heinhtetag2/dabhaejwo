package com.dabhaejwo.domain.plan.dto.response;

import com.dabhaejwo.global.llm.LlmProviderName;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 요금제별 모델 배정. api-contracts.md §7.
 *
 * @param estimatedCostPerConvKrw 조각 수 × 조각당 토큰 추정 × <b>현재 단가</b>로 서버가 계산한 값.
 *                                저장하지 않는다 — 단가가 바뀌면 따라 움직여야 한다.
 *                                단가가 등록돼 있지 않으면 {@code null} 이다.
 */
public record PlanModelAssignmentResponse(
        PlanRef plan,
        LlmProviderName provider,
        String model,
        int chunkCount,
        BigDecimal estimatedCostPerConvKrw) {

    public record PlanRef(UUID id, String name) {
    }
}
