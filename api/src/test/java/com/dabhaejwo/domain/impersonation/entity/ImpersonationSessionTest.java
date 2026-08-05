package com.dabhaejwo.domain.impersonation.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 대리 접속은 남의 고객 데이터를 그대로 열람하는 행위다. 세션이 끝났는지 판단하는
 * 로직이 틀리면 만료된 토큰으로 데이터를 계속 보게 되거나, 업체 화면의 배너가
 * 남아 "아직도 보고 있다"는 오해를 준다.
 */
class ImpersonationSessionTest {

    private ImpersonationSession start(int ttlMinutes) {
        return ImpersonationSession.start(UUID.randomUUID(), UUID.randomUUID(), "문의 #482 재현", ttlMinutes);
    }

    @Test
    @DisplayName("시작하면 ACTIVE 이고 만료는 TTL 뒤다")
    void startsActive() {
        var session = start(30);

        assertEquals(ImpersonationStatus.ACTIVE, session.getStatus());
        assertTrue(session.active(OffsetDateTime.now()));
        assertEquals(30, java.time.Duration.between(
                session.getStartedAt(), session.getExpiresAt()).toMinutes());
    }

    @Test
    @DisplayName("만료 시각이 지나면 상태가 ACTIVE 여도 끝난 세션이다")
    void expiredEvenWhenStatusActive() {
        // 만료 처리 배치가 늦어도 화면은 정확해야 한다.
        var session = start(30);

        assertFalse(session.active(OffsetDateTime.now().plusMinutes(31)));
        assertEquals(ImpersonationStatus.ACTIVE, session.getStatus());
    }

    @Test
    @DisplayName("종료하면 ENDED 로 바뀌고 종료 시각이 남는다")
    void endMarksEnded() {
        var session = start(30);

        session.end();

        assertEquals(ImpersonationStatus.ENDED, session.getStatus());
        assertNotNull(session.getEndedAt());
    }

    @Test
    @DisplayName("이미 끝난 세션을 다시 끝내도 처음 끝난 상태가 유지된다")
    void finishIsIdempotent() {
        var session = start(30);
        session.end();
        OffsetDateTime firstEndedAt = session.getEndedAt();

        // 대상 업체가 해지되어 revoke 가 뒤늦게 불려도 ENDED 를 덮지 않는다.
        session.revoke();

        assertEquals(ImpersonationStatus.ENDED, session.getStatus());
        assertEquals(firstEndedAt, session.getEndedAt());
    }

    @Test
    @DisplayName("연장하면 사유가 갱신되고 만료가 지금부터 다시 잡힌다")
    void extendResetsExpiry() {
        var session = start(30);

        session.extend("추가 확인 필요 — 결제 실패 재현", 30);

        assertEquals("추가 확인 필요 — 결제 실패 재현", session.getReason());
        assertTrue(session.getExpiresAt().isAfter(OffsetDateTime.now().plusMinutes(29)));
    }
}
