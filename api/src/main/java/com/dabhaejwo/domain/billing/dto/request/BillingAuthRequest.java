package com.dabhaejwo.domain.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 결제창이 성공 URL 로 돌려준 값.
 *
 * <p>{@code customerKey} 를 클라이언트에서 받지만 <b>신뢰하지 않는다</b> —
 * 서버가 토큰의 업체와 대조해 다르면 거절한다. 안 그러면 남의 업체에 카드를 붙일 수 있다.
 */
public record BillingAuthRequest(
        @NotBlank @Size(max = 300) String authKey,
        @NotBlank @Size(max = 300) String customerKey) {
}
