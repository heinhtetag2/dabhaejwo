package com.dabhaejwo.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LeadRequest(@NotNull UUID sessionId,
                          @NotBlank @Size(max = 60) String name,
                          @NotBlank @Size(max = 120) String contact,
                          @Size(max = 500) String memo) {
}
