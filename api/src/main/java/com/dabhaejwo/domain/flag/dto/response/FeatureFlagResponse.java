package com.dabhaejwo.domain.flag.dto.response;

import com.dabhaejwo.domain.flag.entity.FeatureFlag;
import com.dabhaejwo.domain.flag.entity.FlagScope;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 기능 플래그. api-contracts.md §8. */
public record FeatureFlagResponse(
        String key,
        String name,
        String description,
        FlagScope scope,
        List<UUID> targetTenantIds,
        List<String> targetTenantNames,
        UUID targetPlanId,
        String targetPlanName,
        boolean enabled,
        OffsetDateTime updatedAt) {

    /**
     * 이름을 함께 싣는 이유는 화면이 "노르드하임 가구 외 2곳"을 보여줘야 하는데,
     * id 만 주면 프론트가 업체 목록을 통째로 받아 조인해야 하기 때문이다.
     */
    public static FeatureFlagResponse of(FeatureFlag flag,
                                         List<String> tenantNames,
                                         String planName) {
        return new FeatureFlagResponse(
                flag.getKey(),
                flag.getName(),
                flag.getDescription(),
                flag.getScope(),
                flag.getTargetTenantIds(),
                tenantNames,
                flag.getTargetPlanId(),
                planName,
                flag.isEnabled(),
                flag.getUpdatedAt());
    }
}
