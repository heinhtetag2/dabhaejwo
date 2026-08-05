package com.dabhaejwo.domain.usage.dto.response;

import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.UsagePurpose;

import java.math.BigDecimal;

/** 모델 × 용도별 사용량. api-contracts.md §6. */
public record ModelUsageResponse(
        LlmProviderName provider,
        String model,
        UsagePurpose purpose,
        long callCount,
        long inputTokens,
        long outputTokens,
        BigDecimal costKrw,
        int sharePercent) {
}
