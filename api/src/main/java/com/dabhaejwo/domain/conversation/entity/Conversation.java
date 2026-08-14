package com.dabhaejwo.domain.conversation.entity;

import com.dabhaejwo.global.security.BotScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 방문자 한 명과의 대화 한 건.
 *
 * <p>방문자 IP 는 원문을 저장하지 않는다 — 해시만 둔다. 레이트 리밋과 중복 판별에는
 * 해시로 충분하고, 원문은 유출됐을 때 되돌릴 수 없다.
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 어느 서비스의 것인가. 조회는 전부 이 값으로 좁힌다. */
    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(name = "started_path")
    private String startedPath;

    @Column(name = "visitor_region")
    private String visitorRegion;

    @Column(name = "visitor_ip_hash")
    private String visitorIpHash;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    protected Conversation() {
    }

    public static Conversation start(BotScope scope, String startedPath, String visitorRegion,
                                     String visitorIpHash) {
        Conversation conversation = new Conversation();
        conversation.tenantId = scope.tenantId();
        conversation.botId = scope.botId();
        conversation.startedPath = startedPath;
        conversation.visitorRegion = visitorRegion;
        conversation.visitorIpHash = visitorIpHash;
        conversation.startedAt = OffsetDateTime.now();
        return conversation;
    }

    public void end() {
        this.endedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getBotId() {
        return botId;
    }

    public String getStartedPath() {
        return startedPath;
    }

    public String getVisitorRegion() {
        return visitorRegion;
    }

    /**
     * 원문 IP 가 아니라 해시다. 같은 방문자인지 묶는 데만 쓰고 되돌릴 수 없다.
     */
    public String getVisitorIpHash() {
        return visitorIpHash;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }
}
