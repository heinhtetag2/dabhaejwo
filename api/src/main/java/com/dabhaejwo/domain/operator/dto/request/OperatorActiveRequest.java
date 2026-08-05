package com.dabhaejwo.domain.operator.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 비활성화·복구. <b>삭제 요청이 아니다</b> — 운영자 삭제 엔드포인트는 존재하지 않는다.
 * 감사 기록이 행위자를 FK 로 참조하고 3년 보존이라 지울 수 없다.
 */
public record OperatorActiveRequest(
        boolean active,
        @NotBlank String reason) {
}
