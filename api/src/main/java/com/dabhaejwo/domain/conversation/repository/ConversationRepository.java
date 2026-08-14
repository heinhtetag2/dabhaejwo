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

    /**
     * 대화 로그 목록. <b>메시지가 하나도 없는 대화는 빼고</b> 준다.
     *
     * <p>대화는 패널을 <b>열 때</b> 만들어진다 — 열어보고 안 물어본 방문자도 알아야 하기
     * 때문이다. 다만 목록에서는 그것들이 "메시지 없음"으로 대부분을 차지해, 정작 읽어야 할
     * 대화를 밀어낸다. 만드는 것은 그대로 두고 <b>보여줄 때만</b> 거른다.
     */
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.botId = :botId
              AND EXISTS (SELECT 1 FROM Message m WHERE m.conversationId = c.id)
            ORDER BY c.startedAt DESC
            """)
    Page<Conversation> findAnsweredByBotId(@Param("botId") UUID botId, Pageable pageable);

    /** 방문자 번호를 매기려면 업체 전체를 처음 온 순서로 봐야 한다 (ConversationService 참조). */
    java.util.List<Conversation> findAllByBotIdOrderByStartedAtAsc(UUID botId);

    Optional<Conversation> findByIdAndBotId(UUID id, UUID botId);

    /**
     * 월 한도 판정. <b>질문이 오간 대화만</b> 센다.
     *
     * <p>전에는 만들어진 대화를 전부 셌다. 그러면 패널을 열기만 해도 한도가 깎여,
     * 방문자가 호기심에 백 번 열면 질문 한 번 없이 그 달 한도가 끝난다 — 봇이면 더 빠르다.
     * 원가가 나가는 것은 질문이므로 한도도 거기에 맞춘다.
     *
     * <p><b>업체 합산이다</b> — 서비스가 여럿이어도 전부 더해 센다. 계약의 단위가 업체이기
     * 때문이고, 이름의 {@code AcrossBots} 가 그 사실을 말한다.
     */
    @Query("""
            SELECT COUNT(c) FROM Conversation c
            WHERE c.tenantId = :tenantId
              AND c.startedAt >= :from AND c.startedAt < :to
              AND EXISTS (SELECT 1 FROM Message m WHERE m.conversationId = c.id)
            """)
    long countAnsweredAcrossBotsBetween(@Param("tenantId") UUID tenantId,
                                        @Param("from") OffsetDateTime from,
                                        @Param("to") OffsetDateTime to);

    /**
     * 서비스 하나의 대화 수. 홈 화면이 쓴다.
     *
     * <p>한도용 {@code countAnsweredAcrossBotsBetween} 과 <b>정의를 맞춘다</b> —
     * 홈이 보여주는 "오늘 대화"와 실제로 깎이는 수가 다르면 업체는 둘 중 무엇도 믿지 못한다.
     * 다만 범위가 다르다: 이쪽은 서비스, 저쪽은 업체 합산이다.
     */
    @Query("""
            SELECT COUNT(c) FROM Conversation c
            WHERE c.botId = :botId
              AND c.startedAt >= :from AND c.startedAt < :to
              AND EXISTS (SELECT 1 FROM Message m WHERE m.conversationId = c.id)
            """)
    long countAnsweredByBotIdBetween(@Param("botId") UUID botId,
                                     @Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to);

    /** 전 업체 대화 수. 운영 콘솔 전용 — 테넌트 조건이 없는 유일한 집계다. */
    long countByStartedAtGreaterThanEqualAndStartedAtLessThan(OffsetDateTime from, OffsetDateTime to);

    /**
     * 일 집계 배치가 읽는다. 하루치를 테넌트별로 묶는다.
     *
     * <p>한도 판정과 <b>같은 정의</b>를 써야 한다 — 화면이 보여주는 "12 / 100" 과 실제로
     * 막히는 시점이 다르면 업체는 둘 중 무엇도 믿지 못한다.
     */
    @Query("""
            SELECT c.tenantId AS tenantId, COUNT(c) AS count FROM Conversation c
            WHERE c.startedAt >= :from AND c.startedAt < :to
              AND EXISTS (SELECT 1 FROM Message m WHERE m.conversationId = c.id)
            GROUP BY c.tenantId
            """)
    java.util.List<TenantCount> countByTenantBetween(@Param("from") OffsetDateTime from,
                                                     @Param("to") OffsetDateTime to);

    /** 같은 정의를 서비스별로. 업체 축 쿼리는 그대로다. */
    @Query("""
            SELECT c.botId AS botId, COUNT(c) AS count FROM Conversation c
            WHERE c.startedAt >= :from AND c.startedAt < :to
              AND EXISTS (SELECT 1 FROM Message m WHERE m.conversationId = c.id)
            GROUP BY c.botId
            """)
    java.util.List<BotCount> countByBotBetween(@Param("from") OffsetDateTime from,
                                               @Param("to") OffsetDateTime to);

    interface BotCount {
        UUID getBotId();

        long getCount();
    }

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
            WHERE c.botId = :botId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY c.startedAt DESC
            """)
    Page<Conversation> search(@Param("botId") UUID botId,
                              @Param("query") String query,
                              Pageable pageable);
}
