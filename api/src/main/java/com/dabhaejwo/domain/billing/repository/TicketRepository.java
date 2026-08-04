package com.dabhaejwo.domain.billing.repository;

import com.dabhaejwo.domain.billing.entity.Ticket;
import com.dabhaejwo.domain.billing.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** 같은 업체가 처리되지 않은 전환 신청을 또 넣지 않게 확인한다. */
    boolean existsByTenantIdAndSubjectAndStatus(UUID tenantId, String subject, TicketStatus status);
}
