package com.dabhaejwo.domain.usage.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 원가 상위 업체. api-contracts.md §6.
 *
 * @param costPerConvKrw 대화당 원가. 이번 달 대화가 없으면 {@code null} 이다
 */
public record TopTenantUsageResponse(
        TenantRef tenant,
        String planName,
        long tokens,
        BigDecimal costKrw,
        BigDecimal costPerConvKrw) {

    public record TenantRef(UUID id, String name) {
    }
}
