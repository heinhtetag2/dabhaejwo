package com.dabhaejwo.domain.billing.dto.response;

import com.dabhaejwo.domain.billing.entity.Ticket;
import com.dabhaejwo.domain.billing.entity.TicketStatus;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문의 한 건. api-contracts.md §8.
 *
 * <p>{@code elapsedMinutes} 를 서버가 계산해 주는 이유는 정렬 기준이 경과 시간이고,
 * 화면이 시계를 따로 굴리면 목록 순서와 표시가 어긋나기 때문이다.
 */
public record TicketResponse(
        Long id,
        TenantRef tenant,
        String subject,
        String body,
        TicketStatus status,
        long elapsedMinutes,
        OperatorRef answeredBy,
        OffsetDateTime answeredAt,
        OffsetDateTime createdAt) {

    public record TenantRef(UUID id, String name) {
    }

    public record OperatorRef(UUID id, String name) {
    }

    public static TicketResponse of(Ticket ticket, TenantRef tenant, OperatorRef answeredBy) {
        return new TicketResponse(
                ticket.getId(),
                tenant,
                ticket.getSubject(),
                ticket.getBody(),
                ticket.getStatus(),
                Duration.between(ticket.getCreatedAt(), OffsetDateTime.now()).toMinutes(),
                answeredBy,
                ticket.getAnsweredAt(),
                ticket.getCreatedAt());
    }
}
