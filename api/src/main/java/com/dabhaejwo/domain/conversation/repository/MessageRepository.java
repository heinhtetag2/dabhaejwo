package com.dabhaejwo.domain.conversation.repository;

import com.dabhaejwo.domain.conversation.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * 답변 실패가 하나라도 있는 대화. 목록에 배지를 달아 개선으로 유도한다.
     * 대화마다 메시지를 세면 N+1 이 되므로 한 번에 모은다.
     */
    @Query("""
            SELECT DISTINCT m.conversationId FROM Message m
            WHERE m.tenantId = :tenantId AND m.conversationId IN :conversationIds
              AND m.answered = false
            """)
    List<UUID> findFailedConversationIds(@Param("tenantId") UUID tenantId,
                                         @Param("conversationIds") List<UUID> conversationIds);

    /**
     * 일 집계 배치가 읽는다. 저장 답변으로 나간 BOT 메시지를 테넌트별로 센다 —
     * 이 건수는 모델 원가가 0 이고 대화 사용량에도 잡히지 않는다.
     */
    @Query("""
            SELECT m.tenantId AS tenantId, COUNT(m) AS count FROM Message m
            WHERE m.saved = true AND m.createdAt >= :from AND m.createdAt < :to
            GROUP BY m.tenantId
            """)
    List<TenantCount> countSavedByTenantBetween(@Param("from") OffsetDateTime from,
                                                @Param("to") OffsetDateTime to);

    /**
     * 답변의 근거 문서를 기록한다.
     *
     * <p>{@code uuid[]} 는 Hibernate 로 매핑하지 않는다({@code Message} 주석 참조).
     * 여기 native update 한 곳만 배열을 안다.
     *
     * <p>이 기록이 없으면 "챗봇이 왜 이렇게 답했나"에 답할 방법이 없다 —
     * 조각은 다시 학습하면 바뀌고, 프롬프트는 어디에도 남지 않는다.
     */
    @Modifying
    @Query(value = "UPDATE messages SET source_document_ids = CAST(:ids AS uuid[]) WHERE id = :messageId",
            nativeQuery = true)
    void attachSourceDocuments(@Param("messageId") UUID messageId, @Param("ids") String ids);

    interface TopQuestion {
        String getQuestion();

        long getAskCount();
    }

    interface TenantCount {
        UUID getTenantId();

        long getCount();
    }
}
