package com.dabhaejwo.domain.conversation.entity;

import com.dabhaejwo.global.security.BotScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 방문자가 누른 👍👎.
 *
 * <p>{@code message_id} 가 곧 PK 다 — 한 답변에 평가는 하나다. 별도 id 를 두면 같은 방문자가
 * 여러 번 눌러 통계가 부풀고, "몇 %가 도움이 됐나"를 믿을 수 없게 된다.
 *
 * <p>👎 가 중요하다. 답변 실패는 챗봇이 스스로 아는 것이고, 👎 는 <b>답은 했는데 틀렸다</b>는
 * 신호다. 후자는 다른 방법으로 알아낼 수 없다.
 */
@Entity
@Table(name = "message_feedback")
public class MessageFeedback {

    @Id
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 어느 서비스의 것인가. 조회는 전부 이 값으로 좁힌다. */
    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(nullable = false)
    private boolean helpful;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected MessageFeedback() {
    }

    public static MessageFeedback of(UUID messageId, BotScope scope, boolean helpful) {
        MessageFeedback feedback = new MessageFeedback();
        feedback.messageId = messageId;
        feedback.tenantId = scope.tenantId();
        feedback.botId = scope.botId();
        feedback.helpful = helpful;
        feedback.createdAt = OffsetDateTime.now();
        return feedback;
    }

    /** 마음이 바뀌면 덮어쓴다. 새 행을 만들지 않는다. */
    public void change(boolean newHelpful) {
        this.helpful = newHelpful;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getMessageId() {
        return messageId;
    }

    public boolean isHelpful() {
        return helpful;
    }
}
