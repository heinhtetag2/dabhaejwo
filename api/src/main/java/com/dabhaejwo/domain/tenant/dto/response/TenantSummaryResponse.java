package com.dabhaejwo.domain.tenant.dto.response;

import com.dabhaejwo.domain.tenant.entity.TenantStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 업체 목록 항목. {@link TenantDetailResponse} 의 부분집합이며
 * 겹치는 필드는 이름·타입이 완전히 동일하다 (api-contract-rules 단일 표현 규칙).
 */
public record TenantSummaryResponse(
        UUID id,
        String name,
        String primaryDomain,
        TenantStatus status,
        PlanRef plan,
        long convCount,
        int convLimit,
        BigDecimal costKrw,
        int billedKrw,
        int costRatioPercent
) {

    public record PlanRef(UUID id, String name) {
    }
}
