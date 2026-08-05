package com.dabhaejwo.domain.notification.entity;

import com.dabhaejwo.global.security.OperatorRole;

import java.util.Set;

/**
 * 알림 종류. <b>대상·중요도·받을 역할을 한 곳에 모은다.</b>
 *
 * <p>흩어 두면 "이 알림을 누가 받나"를 알려면 발행하는 코드를 전부 찾아다녀야 한다.
 * 문구와 이동 경로는 사건마다 달라지므로 발행 시점에 만들고, 여기서는 <b>누구에게 갈지</b>만 정한다.
 *
 * <p>{@code roles} 가 비어 있으면 운영자 전 역할이 받는다. CS 에게 단가 알림,
 * 개발에게 결제 알림은 소음이라 좁힌다 (admin-console-plan.md §7 권한 매트릭스와 같은 결).
 */
public enum NotificationType {

    // ── 운영자 ──────────────────────────────────────────────
    /** 새 업체가 가입했다. */
    TENANT_SIGNED_UP(NotificationAudience.OPS, Severity.LOW, Set.of()),
    /** 문의가 접수됐다. 지금은 유료 전환 신청이 이 경로로 들어온다. */
    TICKET_OPENED(NotificationAudience.OPS, Severity.NORMAL,
            Set.of(OperatorRole.OPS_ADMIN, OperatorRole.CS)),
    /** 전체 일일 원가가 상한에 근접했다. 방어선이 무너지기 전 마지막 경고다. */
    GLOBAL_COST_CAP_WARNING(NotificationAudience.OPS, Severity.HIGH,
            Set.of(OperatorRole.OPS_ADMIN)),
    /** 업체 하나가 일일 상한에 도달해 그 챗봇이 멈췄다. */
    TENANT_COST_CAP_REACHED(NotificationAudience.OPS, Severity.NORMAL,
            Set.of(OperatorRole.OPS_ADMIN, OperatorRole.CS)),
    /** 원가율이 100% 를 넘겼다 — 쓸수록 적자인 계정이 생겼다. */
    TENANT_COST_EXCEEDED(NotificationAudience.OPS, Severity.NORMAL,
            Set.of(OperatorRole.OPS_ADMIN, OperatorRole.SALES)),
    /** 체험이 곧 끝난다. 영업이 연락할 시점이다. */
    TRIAL_ENDING_SOON(NotificationAudience.OPS, Severity.LOW,
            Set.of(OperatorRole.OPS_ADMIN, OperatorRole.SALES)),
    /** 문서 학습 실패가 쌓였다. */
    INDEXING_FAILURES(NotificationAudience.OPS, Severity.NORMAL,
            Set.of(OperatorRole.OPS_ADMIN, OperatorRole.CS, OperatorRole.DEV)),
    /**
     * 결제가 들어왔다.
     *
     * <p>TODO(stub): PG(토스페이먼츠) 미연동이라 <b>아직 아무도 발행하지 않는다.</b>
     * 자리를 미리 둔 이유는 결제를 붙일 때 알림 쪽을 다시 설계하지 않기 위해서다.
     */
    PAYMENT_RECEIVED(NotificationAudience.OPS, Severity.NORMAL,
            Set.of(OperatorRole.OPS_ADMIN, OperatorRole.SALES)),

    // ── 업체 ────────────────────────────────────────────────
    /**
     * 운영팀이 대리 접속했다.
     *
     * <p>사후 목록보다 실시간 알림이 훨씬 강한 신뢰 장치다 (tenant-plan.md §6.3).
     * <b>이 알림은 운영자가 읽음 처리할 수 없다</b> — 자기 접속을 자기가 지우는 셈이 된다.
     */
    IMPERSONATION_STARTED(NotificationAudience.TENANT, Severity.HIGH, Set.of()),
    /** 이번 달 대화 한도의 80% 를 썼다. */
    QUOTA_WARNING(NotificationAudience.TENANT, Severity.NORMAL, Set.of()),
    /** 한도를 다 썼다. 챗봇이 멈춘다 — 갑자기 멈추는 경험은 해지로 직결된다. */
    QUOTA_EXHAUSTED(NotificationAudience.TENANT, Severity.HIGH, Set.of()),
    /** 무료 체험이 곧 끝난다. */
    TRIAL_ENDING(NotificationAudience.TENANT, Severity.HIGH, Set.of()),
    /** 문서 학습이 끝났다. */
    INDEXING_DONE(NotificationAudience.TENANT, Severity.LOW, Set.of()),
    /** 문서 학습이 실패했다. */
    INDEXING_FAILED(NotificationAudience.TENANT, Severity.NORMAL, Set.of()),
    /** 방문자가 연락처를 남겼다 — 업체 입장에서 이게 곧 매출이다. */
    LEAD_RECEIVED(NotificationAudience.TENANT, Severity.NORMAL, Set.of()),
    /** 챗봇이 답하지 못한 질문이 쌓였다. */
    ANSWER_GAPS_PILING(NotificationAudience.TENANT, Severity.NORMAL, Set.of());

    private final NotificationAudience audience;
    private final Severity severity;
    private final Set<OperatorRole> roles;

    NotificationType(NotificationAudience audience, Severity severity, Set<OperatorRole> roles) {
        this.audience = audience;
        this.severity = severity;
        this.roles = roles;
    }

    public NotificationAudience audience() {
        return audience;
    }

    public Severity severity() {
        return severity;
    }

    /** 비어 있으면 전 역할. 업체 알림은 항상 비어 있다(역할이 아니라 테넌트로 나뉜다). */
    public Set<OperatorRole> roles() {
        return roles;
    }

    public enum Severity {
        LOW,
        NORMAL,
        HIGH
    }
}
