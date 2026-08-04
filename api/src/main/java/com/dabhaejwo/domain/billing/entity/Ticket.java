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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Ticket() {
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

    public String getSubject() {
        return subject;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
