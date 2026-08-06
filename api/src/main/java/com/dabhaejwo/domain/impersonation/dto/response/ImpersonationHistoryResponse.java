package com.dabhaejwo.domain.impersonation.dto.response;

import com.dabhaejwo.domain.impersonation.entity.ImpersonationSession;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체가 보는 운영팀 접속 이력.
 *
 * <p><b>상태는 시간까지 보고 정한다.</b> DB 의 {@code status} 는 운영자가 <b>명시적으로 끝냈을
 * 때만</b> 바뀐다 — 30분이 지나 저절로 만료된 세션은 {@code ACTIVE} 인 채로 남는다. 그 값을
 * 그대로 내보내면 아무도 접속해 있지 않은데 업체 화면에 "접속 중"이 줄줄이 뜬다.
 * 신뢰를 얻으려고 공개하는 화면에서 거짓말을 하는 셈이라 특히 나쁘다.
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
        return from(session, OffsetDateTime.now());
    }

    /** @param now 만료 판정 기준 시각. 테스트가 시간을 고정할 수 있도록 받는다 */
    public static ImpersonationHistoryResponse from(ImpersonationSession session, OffsetDateTime now) {
        return new ImpersonationHistoryResponse(
                session.getId(),
                session.getReason(),
                session.getStartedAt(),
                session.getEndedAt(),
                effectiveStatus(session, now));
    }

    /**
     * 실제 상태. {@code ACTIVE} 로 적혀 있어도 시간이 지났으면 만료다.
     *
     * <p>DB 를 고쳐 쓸 수도 있지만 그러려면 만료를 훑는 배치가 필요하고, 그 배치가 한 번
     * 멈추면 화면이 다시 거짓말을 한다. 읽을 때 판정하면 그런 구멍이 없다.
     */
    private static ImpersonationStatus effectiveStatus(ImpersonationSession session, OffsetDateTime now) {
        if (session.active(now)) {
            return ImpersonationStatus.ACTIVE;
        }
        // 운영자가 직접 끝낸 것과 시간이 지나 풀린 것은 다른 사실이다.
        // 둘을 뭉뚱그리면 "왜 아직 열려 있었나"를 업체가 물었을 때 답할 근거가 사라진다.
        return session.getStatus() == ImpersonationStatus.ENDED
                ? ImpersonationStatus.ENDED
                : ImpersonationStatus.EXPIRED;
    }
}
