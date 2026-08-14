package com.dabhaejwo.domain.impersonation.dto.response;

import com.dabhaejwo.domain.impersonation.entity.ImpersonationSession;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 운영자가 받는 대리 접속 세션. api-contracts.md §3.
 *
 * <p>{@code accessToken} 은 <b>VIEWER 권한</b>으로 발급된다 — 운영자가 남의 설정을
 * 바꾸는 일이 없게 서버가 낮춰 준다. 세션 종료 응답에는 토큰이 없다(null).
 */
public record ImpersonationSessionResponse(
        UUID sessionId,
        TenantRef tenant,
        String reason,
        String accessToken,
        /**
         * 업체 대시보드에서 열 경로. <b>서버가 만든다.</b>
         *
         * <p>admin·tenant 두 프로젝트가 각자 URL 규칙을 적기 시작하면 갈린다 —
         * 알림 경로를 서버가 만드는 것과 같은 이유다. 세션 종료 응답에는 없다(null).
         */
        String entryPath,
        OffsetDateTime startedAt,
        OffsetDateTime expiresAt,
        ImpersonationStatus status) {

    public record TenantRef(UUID id, String name) {
    }

    public static ImpersonationSessionResponse of(ImpersonationSession session,
                                                  TenantRef tenant,
                                                  String accessToken,
                                                  String entryPath) {
        return new ImpersonationSessionResponse(
                session.getId(),
                tenant,
                session.getReason(),
                accessToken,
                entryPath,
                session.getStartedAt(),
                session.getExpiresAt(),
                session.getStatus());
    }
}
