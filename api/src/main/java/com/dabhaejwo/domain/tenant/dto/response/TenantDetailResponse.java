package com.dabhaejwo.domain.tenant.dto.response;

import com.dabhaejwo.domain.tenant.entity.TenantStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체 상세. Summary 의 상위집합이다.
 *
 * <p>{@code faqCount} 가 0이면 프론트가 강조 표시한다 — 원가 급증의 선행 지표다.
 */
public record TenantDetailResponse(
        UUID id,
        String name,
        String primaryDomain,
        String publishableKey,
        TenantStatus status,
        String currency,
        PlanDetail plan,
        long convCount,
        int convLimit,
        long docCount,
        int docLimit,
        long faqCount,
        int savedAnswerPercent,
        BigDecimal costKrw,
        int billedKrw,
        int costRatioPercent,
        LocalDate joinedDate,
        OffsetDateTime trialEndsAt,
        LocalDate nextBillingDate,
        OffsetDateTime lastSeenAt
) {

    public record PlanDetail(UUID id, String name, int monthlyFee) {
    }
}
