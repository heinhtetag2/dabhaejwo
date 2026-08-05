package com.dabhaejwo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record OtpVerifyRequest(
        @NotNull UUID challengeId,
        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "인증 코드는 6자리 숫자입니다") String code) {
}
