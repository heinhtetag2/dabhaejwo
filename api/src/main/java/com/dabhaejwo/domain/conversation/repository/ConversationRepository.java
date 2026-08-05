package com.dabhaejwo.domain.conversation.repository;

import com.dabhaejwo.domain.conversation.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findAllByTenantIdOrderByStartedAtDesc(UUID tenantId, Pageable pageable);

    Optional<Conversation> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantIdAndStartedAtBetween(UUID tenantId, OffsetDateTime from, OffsetDateTime to);

    /** 전 업체 대화 수. 운영 콘솔 전용 — 테넌트 조건이 없는 유일한 집계다. */
    long countByStartedAtGreaterThanEqualAndStartedAtLessThan(OffsetDateTime from, OffsetDateTime to);

    /** 일 집계 배치가 읽는다. 하루치를 테넌트별로 묶는다. */
    @Query("""
            SELECT c.tenantId AS tenantId, COUNT(c) AS count FROM Conversation c
            WHERE c.startedAt >= :from AND c.startedAt < :to
            GROUP BY c.tenantId
            """)
    java.util.List<TenantCount> countByTenantBetween(@Param("from") OffsetDateTime from,
                                                     @Param("to") OffsetDateTime to);

    interface TenantCount {
        UUID getTenantId();

        long getCount();
    }

    /**
     * 대화 내용 검색. 메시지 본문을 훑어야 하므로 조인한다.
     * 테넌트 조건이 항상 붙는다 — 타 업체 대화가 섞이면 P0 이다.
     */
    @Query("""
            SELECT DISTINCT c FROM Conversation c
            JOIN Message m ON m.conversationId = c.id
            WHERE c.tenantId = :tenantId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY c.startedAt DESC
            """)
    Page<Conversation> search(@Param("tenantId") UUID tenantId,
                              @Param("query") String query,
                              Pageable pageable);
}
