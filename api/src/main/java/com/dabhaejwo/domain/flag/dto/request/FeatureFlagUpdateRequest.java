package com.dabhaejwo.domain.flag.dto.request;

import com.dabhaejwo.domain.flag.entity.FlagScope;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * {@code key} 를 받지 않는다 — 경로에 있고, 바꿀 수 있는 값이 아니다.
 * 코드가 그 문자열로 기능을 찾으므로 바뀌면 기능이 조용히 꺼진다.
 */
public record FeatureFlagUpdateRequest(
        @NotNull FlagScope scope,
        List<UUID> targetTenantIds,
        UUID targetPlanId,
        boolean enabled) {
}
