package com.dabhaejwo.domain.lead.entity;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.BotScope;
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
 * 방문자가 남긴 연락처.
 *
 * <p>{@code contact} 는 원문으로 저장하되 <b>화면에는 마스킹해서 내보낸다.</b>
 * 원문은 업체가 명시적으로 CSV 를 내려받을 때만 나간다 (admin-console-plan §8).
 */
@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 어느 서비스의 것인가. 조회는 전부 이 값으로 좁힌다. */
    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contact;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Lead() {
    }

    public static Lead of(BotScope scope, UUID conversationId, String name, String contact, String reason) {
        Lead lead = new Lead();
        lead.tenantId = scope.tenantId();
        lead.botId = scope.botId();
        lead.conversationId = conversationId;
        lead.name = name;
        lead.contact = contact;
        lead.reason = reason;
        lead.status = LeadStatus.NEW;
        lead.createdAt = OffsetDateTime.now();
        return lead;
    }

    /** 되돌리기는 허용한다 — 잘못 눌렀을 때 손쓸 방법이 없으면 안 된다. CLOSED 만 종착이다. */
    public void changeStatus(LeadStatus next) {
        if (this.status == LeadStatus.CLOSED && next != LeadStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        this.status = next;
    }

    /**
     * 가운데를 가린 연락처. 전화번호든 이메일이든 뒷부분을 남겨 본인 확인은 되게 한다.
     */
    public String maskedContact() {
        return mask(contact);
    }

    static String mask(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        int at = raw.indexOf('@');
        if (at > 0) {
            String local = raw.substring(0, at);
            String head = local.substring(0, Math.min(2, local.length()));
            return head + "*".repeat(Math.max(1, local.length() - head.length())) + raw.substring(at);
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() < 7) {
            return "*".repeat(raw.length());
        }
        String head = digits.substring(0, 3);
        String tail = digits.substring(digits.length() - 4);
        return head + "-****-" + tail;
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

    public UUID getConversationId() {
        return conversationId;
    }

    public String getName() {
        return name;
    }

    /** 원문. CSV 내보내기 전용 — 목록 응답에 쓰지 않는다. */
    public String getContactRaw() {
        return contact;
    }

    public String getReason() {
        return reason;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
