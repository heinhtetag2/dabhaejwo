package com.dabhaejwo.domain.tenant.dto.response;

import com.dabhaejwo.domain.tenant.entity.QuotaOverride;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** 쿼터 증량 이력. api-contracts.md §4. */
public record QuotaOverrideResponse(
        Long id,
        String periodMonth,
        int convDelta,
        int docDelta,
        String reason,
        OperatorRef operator,
        OffsetDateTime createdAt) {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    public record OperatorRef(UUID id, String name) {
    }

    public static QuotaOverrideResponse of(QuotaOverride override, OperatorRef operator) {
        return new QuotaOverrideResponse(
                override.getId(),
                override.getPeriod().format(MONTH),
                override.getConvDelta(),
                override.getDocDelta(),
                override.getReason(),
                operator,
                override.getCreatedAt());
    }
}
