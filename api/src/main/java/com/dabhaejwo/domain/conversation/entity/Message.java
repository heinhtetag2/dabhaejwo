package com.dabhaejwo.domain.conversation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 대화의 메시지 한 줄.
 *
 * <p>{@code source_document_ids uuid[]} 컬럼은 매핑하지 않는다 — 목록·상세 화면이
 * 근거 문서 제목을 문서 테이블에서 따로 읽으므로 배열 매핑을 들일 이유가 없다.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false)
    private String content;

    /** BOT 메시지만 의미가 있다. false 면 답변 실패이며 answer_gaps 로 올라간다. */
    private Boolean answered;

    /** 저장 답변(FAQ)으로 나갔는가. true 면 모델을 거치지 않았다. */
    @Column(nullable = false)
    private boolean saved;

    @Column(name = "faq_id")
    private UUID faqId;

    /**
     * 답을 만드는 데 걸린 시간. 답변 파이프라인이 채운다.
     * 잰 적이 없으면 null 이다 — 0 으로 채우면 "0ms 에 답했다"는 거짓이 된다.
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Message() {
    }

    public static Message fromVisitor(UUID tenantId, UUID conversationId, String content) {
        return fromVisitor(tenantId, conversationId, content, OffsetDateTime.now());
    }

    /**
     * 시각을 직접 받는 형태.
     *
     * <p>질문과 답변을 각각 {@code now()} 로 찍으면 <b>같은 순간에 걸려 순서가 뒤집힌다.</b>
     * 실제로 대화 로그에 답변이 질문보다 위에 나왔다. 답변 시각은 질문 시각 + 걸린 시간이
     * 되어야 하고, 그게 사실이기도 하다.
     */
    public static Message fromVisitor(UUID tenantId, UUID conversationId, String content,
                                      OffsetDateTime askedAt) {
        Message message = new Message();
        message.tenantId = tenantId;
        message.conversationId = conversationId;
        message.role = MessageRole.VISITOR;
        message.content = content;
        message.createdAt = askedAt;
        return message;
    }

    public static Message fromBot(UUID tenantId, UUID conversationId, String content,
                                  boolean answered, boolean saved, UUID faqId) {
        return fromBot(tenantId, conversationId, content, answered, saved, faqId, OffsetDateTime.now());
    }

    public static Message fromBot(UUID tenantId, UUID conversationId, String content,
                                  boolean answered, boolean saved, UUID faqId,
                                  OffsetDateTime answeredAt) {
        Message message = new Message();
        message.tenantId = tenantId;
        message.conversationId = conversationId;
        message.role = MessageRole.BOT;
        message.content = content;
        message.answered = answered;
        message.saved = saved;
        message.faqId = faqId;
        message.createdAt = answeredAt;
        return message;
    }

    /** 답을 만드는 데 걸린 시간을 기록한다. 재보지 않았으면 부르지 않는다 — 0 은 거짓말이다. */
    public void measured(int millis) {
        this.latencyMs = millis;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public boolean isSaved() {
        return saved;
    }

    public UUID getFaqId() {
        return faqId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
