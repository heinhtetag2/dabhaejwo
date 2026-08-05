package com.dabhaejwo.domain.tenant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TenantPlanChangeRequest(
        @NotNull UUID planId,
        @NotBlank String reason) {
}
