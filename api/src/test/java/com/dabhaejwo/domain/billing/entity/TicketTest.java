package com.dabhaejwo.domain.billing.entity;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTest {

    private Ticket open() {
        return Ticket.open(UUID.randomUUID(), "PDF 업로드 실패", "계속 실패로 뜹니다");
    }

    @Test
    @DisplayName("답변 처리하면 처리자와 시각이 남는다")
    void answerRecordsOperator() {
        Ticket ticket = open();
        UUID operatorId = UUID.randomUUID();

        ticket.changeStatus(TicketStatus.ANSWERED, operatorId);

        assertEquals(TicketStatus.ANSWERED, ticket.getStatus());
        assertEquals(operatorId, ticket.getAnsweredBy());
        assertNotNull(ticket.getAnsweredAt());
    }

    @Test
    @DisplayName("종료된 문의는 다시 열 수 없다 — 새 문의는 새 티켓이다")
    void closedIsTerminal() {
        Ticket ticket = open();
        ticket.changeStatus(TicketStatus.CLOSED, UUID.randomUUID());

        BusinessException error = assertThrows(BusinessException.class,
                () -> ticket.changeStatus(TicketStatus.OPEN, UUID.randomUUID()));

        assertEquals(ErrorCode.INVALID_STATE_TRANSITION, error.errorCode());
    }

    @Test
    @DisplayName("새 문의는 처리자가 비어 있다")
    void openHasNoOperator() {
        Ticket ticket = open();

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNull(ticket.getAnsweredBy());
        assertNull(ticket.getAnsweredAt());
    }
}
