package com.dabhaejwo.domain.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 유료 전환 신청.
 *
 * @param planCode 원하는 요금제 코드
 * @param note     연락 시각 희망 등 자유 서술. 없어도 된다
 */
public record UpgradeRequestBody(
        @NotBlank @Size(max = 40) String planCode,
        @Size(max = 1000) String note) {
}
