package com.dabhaejwo.domain.impersonation.dto.response;

import com.dabhaejwo.domain.impersonation.entity.ImpersonationSession;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 업체가 보는 접속 이력.
 *
 * <p>신뢰를 얻으려고 공개하는 화면이라 <b>여기서 틀리면 공개한 것이 오히려 해가 된다</b> —
 * 아무도 안 들어와 있는데 "접속 중"이 줄줄이 뜨면 업체는 우리가 상주한다고 읽는다.
 */
class ImpersonationHistoryResponseTest {

    private static final int TTL = 30;

    private static ImpersonationSession session() {
        return ImpersonationSession.start(UUID.randomUUID(), UUID.randomUUID(), "문의 확인", TTL);
    }

    @Test
    @DisplayName("만료 전에는 접속 중이다")
    void activeBeforeExpiry() {
        ImpersonationSession session = session();

        ImpersonationHistoryResponse response =
                ImpersonationHistoryResponse.from(session, OffsetDateTime.now().plusMinutes(10));

        assertEquals(ImpersonationStatus.ACTIVE, response.status());
    }

    @Test
    @DisplayName("시간이 지나면 만료로 보인다 — DB 값이 ACTIVE 여도")
    void expiresWithTime() {
        // 저절로 만료된 세션은 status 가 ACTIVE 인 채로 남는다. 그것을 그대로 내보내던 것이
        // 어제 세션까지 "접속 중"으로 뜨던 원인이다.
        ImpersonationSession session = session();
        assertEquals(ImpersonationStatus.ACTIVE, session.getStatus());

        ImpersonationHistoryResponse response =
                ImpersonationHistoryResponse.from(session, OffsetDateTime.now().plusMinutes(TTL + 1));

        assertEquals(ImpersonationStatus.EXPIRED, response.status());
    }

    @Test
    @DisplayName("운영자가 직접 끝낸 것은 만료와 구분한다")
    void endedIsNotExpired() {
        ImpersonationSession session = session();
        session.end();

        ImpersonationHistoryResponse response =
                ImpersonationHistoryResponse.from(session, OffsetDateTime.now().plusMinutes(1));

        assertEquals(ImpersonationStatus.ENDED, response.status());
    }
}
