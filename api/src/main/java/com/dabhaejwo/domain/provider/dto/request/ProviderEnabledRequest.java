package com.dabhaejwo.domain.provider.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 공급사 사용 중지·재개. 키는 지우지 않는다 — 잠깐 껐다 되돌리는 경우가 있다. */
public record ProviderEnabledRequest(
        boolean enabled,
        @NotBlank String reason) {
}
