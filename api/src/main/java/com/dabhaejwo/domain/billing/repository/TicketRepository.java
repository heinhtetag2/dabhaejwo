package com.dabhaejwo.domain.billing.repository;

import com.dabhaejwo.domain.billing.entity.Ticket;
import com.dabhaejwo.domain.billing.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** 같은 업체가 처리되지 않은 전환 신청을 또 넣지 않게 확인한다. */
    boolean existsByTenantIdAndSubjectAndStatus(UUID tenantId, String subject, TicketStatus status);

    /**
     * 문의 목록. 정렬은 <b>경과 시간 내림차순 고정</b>이다 — 오래된 것이 위로 온다
     * (admin-console-plan.md §4.9). 정렬을 고르게 하지 않는 이유는, 고를 수 있으면
     * 언젠가 최신순으로 보다가 오래된 문의를 놓치기 때문이다.
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:tenantId IS NULL OR t.tenantId = :tenantId)
            ORDER BY t.createdAt ASC
            """)
    Page<Ticket> search(@Param("status") TicketStatus status,
                        @Param("tenantId") UUID tenantId,
                        Pageable pageable);

    long countByStatus(TicketStatus status);
}
