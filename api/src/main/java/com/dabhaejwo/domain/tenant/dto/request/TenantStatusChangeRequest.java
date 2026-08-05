package com.dabhaejwo.domain.tenant.dto.request;

import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 상태 변경. {@code reason} 은 여기서 {@code @NotBlank} 로 막지 않는다 —
 * 사유가 필요한지는 목표 상태에 따라 다르고(ACTIVE 복구는 불필요),
 * 그 판단은 서비스 레이어에 한 곳으로 모은다.
 */
public record TenantStatusChangeRequest(
        @NotNull TenantStatus status,
        String reason) {
}
