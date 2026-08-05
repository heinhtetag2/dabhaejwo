package com.dabhaejwo.domain.provider.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 공급사 키 등록·교체.
 *
 * <p>{@code reason} 을 요구하는 이유는 단가·안전장치와 같다 — 키 교체는 전 업체의 답변을
 * 즉시 멈추거나 되살릴 수 있는 조작이다. 누가 왜 바꿨는지 남아야 한다.
 */
public record ProviderCredentialRequest(
        @NotBlank @Size(min = 8, max = 500) String apiKey,
        @NotBlank String reason) {
}
