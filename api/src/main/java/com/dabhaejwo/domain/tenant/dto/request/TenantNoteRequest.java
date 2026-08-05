package com.dabhaejwo.domain.tenant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantNoteRequest(
        @NotBlank @Size(max = 4000) String body) {
}
