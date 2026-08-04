package com.dabhaejwo.domain.conversation.repository;

import com.dabhaejwo.domain.conversation.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findAllByTenantIdAndConversationIdOrderByCreatedAtAsc(UUID tenantId, UUID conversationId);

    /** 대화 목록의 미리보기 — 각 대화의 첫 방문자 발화. */
    @Query("""
            SELECT m FROM Message m
            WHERE m.tenantId = :tenantId AND m.conversationId IN :conversationIds
              AND m.role = com.dabhaejwo.domain.conversation.entity.MessageRole.VISITOR
            ORDER BY m.createdAt ASC
            """)
    List<Message> findVisitorMessages(@Param("tenantId") UUID tenantId,
                                      @Param("conversationIds") List<UUID> conversationIds);

    /**
     * 답변 성공률. BOT 메시지 중 answered 가 true 인 비율이다.
     * 저장 답변(saved)도 성공으로 센다 — 방문자 입장에서는 답을 받은 것이다.
     */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.tenantId = :tenantId
              AND m.role = com.dabhaejwo.domain.conversation.entity.MessageRole.BOT
              AND m.createdAt >= :from AND m.createdAt < :to
              AND (:answeredOnly = false OR m.answered = true)
            """)
    long countBotMessages(@Param("tenantId") UUID tenantId,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to,
                          @Param("answeredOnly") boolean answeredOnly);

    /** 홈의 "많이 물어본 질문". 방문자 발화를 그대로 묶는다. */
    @Query("""
            SELECT m.content AS question, COUNT(m) AS askCount FROM Message m
            WHERE m.tenantId = :tenantId
              AND m.role = com.dabhaejwo.domain.conversation.entity.MessageRole.VISITOR
              AND m.createdAt >= :from
            GROUP BY m.content ORDER BY COUNT(m) DESC
            """)
    List<TopQuestion> findTopQuestions(@Param("tenantId") UUID tenantId,
                                       @Param("from") OffsetDateTime from,
                                       org.springframework.data.domain.Pageable pageable);

    /**
     * 평균 응답 시간(ms). 저장 답변은 모델을 거치지 않아 거의 0ms 라 제외한다 —
     * 함께 평균 내면 실제 체감보다 좋게 나온다.
     *
     * @return 잰 메시지가 하나도 없으면 null
     */
    @Query("""
            SELECT AVG(m.latencyMs) FROM Message m
            WHERE m.tenantId = :tenantId AND m.latencyMs IS NOT NULL AND m.saved = false
              AND m.createdAt >= :from
            """)
    Double averageLatency(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from);

    interface TopQuestion {
        String getQuestion();

        long getAskCount();
    }
}
