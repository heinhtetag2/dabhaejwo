package com.dabhaejwo.domain.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문의 티켓.
 *
 * <p>PG 가 연동되기 전까지 <b>유료 전환도 이 경로로 들어온다</b> — 결제 화면을 만들어
 * 카드번호를 받는 시늉을 하는 대신, 신청을 접수하고 운영팀이 수동으로 처리한다
 * (docs/plan/tenant-public-plan.md §5.2).
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "answered_by")
    private UUID answeredBy;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Ticket() {
    }

    /**
     * 상태 변경. 답변 본문은 여기 남기지 않는다 — 회신은 이메일로 나가고, 티켓에 사본을
     * 두면 두 곳이 갈라진다. 여기 남는 것은 "누가 언제 처리했는가"다.
     *
     * <p>{@code CLOSED} 에서 되돌리지 않는다. 새 문의는 새 티켓이다.
     */
    public void changeStatus(TicketStatus target, UUID operatorId) {
        if (status == TicketStatus.CLOSED) {
            throw new com.dabhaejwo.global.exception.BusinessException(
                    com.dabhaejwo.global.exception.ErrorCode.INVALID_STATE_TRANSITION,
                    "종료된 문의는 다시 열 수 없습니다");
        }
        this.status = target;
        if (target == TicketStatus.ANSWERED || target == TicketStatus.CLOSED) {
            this.answeredBy = operatorId;
            this.answeredAt = OffsetDateTime.now();
        }
    }

    public static Ticket open(UUID tenantId, String subject, String body) {
        Ticket ticket = new Ticket();
        ticket.tenantId = tenantId;
        ticket.subject = subject;
        ticket.body = body;
        ticket.status = TicketStatus.OPEN;
        ticket.createdAt = OffsetDateTime.now();
        return ticket;
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public UUID getAnsweredBy() {
        return answeredBy;
    }

    public OffsetDateTime getAnsweredAt() {
        return answeredAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
