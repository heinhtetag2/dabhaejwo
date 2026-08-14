package com.dabhaejwo.domain.impersonation.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 대리 접속 사유. 연장할 때도 같은 형태로 다시 받는다.
 *
 * <p>{@code @NotBlank} 는 공백만 입력한 경우도 거부한다 (tenant-plan.md §6.1).
 * 서비스 레이어의 {@code AuditLogService} 와 DB CHECK 가 같은 조건을 한 번 더 건다 —
 * 이 사유는 업체에게 공개되므로 빈 값이 새어나가면 신뢰 장치가 무너진다.
 */
public record ImpersonationRequest(
        @NotBlank String reason,
        /**
         * 들어갈 서비스. <b>선택이다</b> — 업체 상세의 조치 카드에서 누르면 비어 있고,
         * 서비스 카드의 행에서 누르면 그 서비스가 실린다.
         *
         * <p>비어 있으면 기본 서비스로 들어간다. 어느 쪽이든 감사 기록에는 실제로 들어간
         * 서비스가 남는다.
         */
        java.util.UUID botId) {
}
