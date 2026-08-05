package com.dabhaejwo.domain.tenant.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 쿼터 임시 증량. 이번 달에만 적용된다 — {@code periodMonth} 를 받지 않는 이유는
 * 지난달 한도를 지금 올려도 아무 일도 일어나지 않고, 다음 달을 미리 올리는 것은
 * 요금제 변경으로 해야 할 일이기 때문이다.
 *
 * <p>음수를 허용한다. 잘못 넣은 증량을 되돌리는 유일한 방법이다 — 이력은 지우지 않는다.
 */
public record QuotaOverrideRequest(
        int convDelta,
        int docDelta,
        @NotBlank String reason) {
}
