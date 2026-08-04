package com.dabhaejwo.domain.impersonation.dto.response;

import com.dabhaejwo.domain.impersonation.entity.ImpersonationSession;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체가 보는 운영팀 접속 이력.
 *
 * <p><b>운영자 개인을 특정하지 않는다.</b> 업체에게 필요한 것은 "언제, 왜 들어왔는가"이지
 * 누가 들어왔는지가 아니다. 운영자 id 를 공개하면 개인이 특정되고, 그 정보로 업체가
 * 할 수 있는 일도 없다. 운영자 단위 추적은 감사 기록(운영 콘솔)에 남는다.
 */
public record ImpersonationHistoryResponse(
        UUID sessionId,
        String reason,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        ImpersonationStatus status) {

    public static ImpersonationHistoryResponse from(ImpersonationSession session) {
        return new ImpersonationHistoryResponse(
                session.getId(),
                session.getReason(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getStatus());
    }
}
