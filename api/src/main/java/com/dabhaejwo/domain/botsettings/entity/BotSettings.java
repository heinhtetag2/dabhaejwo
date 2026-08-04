package com.dabhaejwo.domain.botsettings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 챗봇의 말투·모양·설치 설정. 업체당 하나이며 {@code tenant_id} 가 곧 PK 다.
 *
 * <p>별도 id 를 두면 "설정이 두 벌 생긴" 상태가 표현 가능해지고, 그러면 언젠가 실제로 생긴다.
 */
@Entity
@Table(name = "bot_settings")
public class BotSettings {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "bot_name", nullable = false)
    private String botName;

    @Column(name = "brand_color", nullable = false)
    private String brandColor;

    @Column(nullable = false)
    private String greeting;

    /** 시스템 프롬프트로 들어간다. 짧고 구체적일수록 모델이 잘 따른다. */
    @Column(nullable = false)
    private String persona;

    @Column(name = "fallback_message", nullable = false)
    private String fallbackMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "forbidden_topics", columnDefinition = "jsonb", nullable = false)
    private List<String> forbiddenTopics = new ArrayList<>();

    @Column(name = "lead_capture_enabled", nullable = false)
    private boolean leadCaptureEnabled;

    @Column(name = "support_phone")
    private String supportPhone;

    @Column(name = "agent_handoff_enabled", nullable = false)
    private boolean agentHandoffEnabled;

    @Column(name = "agent_hours")
    private String agentHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_position", nullable = false)
    private WidgetPosition widgetPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "page_scope", nullable = false)
    private PageScope pageScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "page_patterns", columnDefinition = "jsonb", nullable = false)
    private List<String> pagePatterns = new ArrayList<>();

    /** 0 이면 자동으로 말 걸지 않는다. */
    @Column(name = "nudge_delay_seconds", nullable = false)
    private int nudgeDelaySeconds;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected BotSettings() {
    }

    /** 업체를 만들 때 함께 만든다. 설정이 없는 상태를 화면이 다루지 않아도 되게 한다. */
    public static BotSettings defaults(UUID tenantId, String tenantName) {
        BotSettings settings = new BotSettings();
        settings.tenantId = tenantId;
        settings.botName = tenantName + " 도우미";
        settings.brandColor = "#17222E";
        settings.greeting = "안녕하세요! 무엇을 도와드릴까요?";
        settings.persona = "";
        settings.fallbackMessage = "제가 확인하기 어려운 내용이네요. 상담원에게 연결해 드릴까요?";
        settings.leadCaptureEnabled = true;
        settings.agentHandoffEnabled = false;
        settings.widgetPosition = WidgetPosition.BOTTOM_RIGHT;
        settings.pageScope = PageScope.ALL;
        settings.nudgeDelaySeconds = 15;
        settings.updatedAt = OffsetDateTime.now();
        return settings;
    }

    public void editAppearance(String botName, String brandColor, String greeting) {
        this.botName = botName;
        this.brandColor = brandColor;
        this.greeting = greeting;
        touch();
    }

    public void editTone(String persona, String fallbackMessage, List<String> forbiddenTopics) {
        this.persona = persona;
        this.fallbackMessage = fallbackMessage;
        this.forbiddenTopics = forbiddenTopics == null ? new ArrayList<>() : new ArrayList<>(forbiddenTopics);
        touch();
    }

    public void editFallback(boolean leadCaptureEnabled,
                             String supportPhone,
                             boolean agentHandoffEnabled,
                             String agentHours) {
        this.leadCaptureEnabled = leadCaptureEnabled;
        this.supportPhone = supportPhone;
        this.agentHandoffEnabled = agentHandoffEnabled;
        this.agentHours = agentHours;
        touch();
    }

    public void editPlacement(WidgetPosition position,
                              PageScope scope,
                              List<String> patterns,
                              int nudgeDelaySeconds) {
        if (nudgeDelaySeconds < 0) {
            throw new IllegalArgumentException("nudgeDelaySeconds must be >= 0");
        }
        this.widgetPosition = position;
        this.pageScope = scope;
        this.pagePatterns = patterns == null ? new ArrayList<>() : new ArrayList<>(patterns);
        this.nudgeDelaySeconds = nudgeDelaySeconds;
        touch();
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getBotName() {
        return botName;
    }

    public String getBrandColor() {
        return brandColor;
    }

    public String getGreeting() {
        return greeting;
    }

    public String getPersona() {
        return persona;
    }

    public String getFallbackMessage() {
        return fallbackMessage;
    }

    public List<String> getForbiddenTopics() {
        return forbiddenTopics;
    }

    public boolean isLeadCaptureEnabled() {
        return leadCaptureEnabled;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public boolean isAgentHandoffEnabled() {
        return agentHandoffEnabled;
    }

    public String getAgentHours() {
        return agentHours;
    }

    public WidgetPosition getWidgetPosition() {
        return widgetPosition;
    }

    public PageScope getPageScope() {
        return pageScope;
    }

    public List<String> getPagePatterns() {
        return pagePatterns;
    }

    public int getNudgeDelaySeconds() {
        return nudgeDelaySeconds;
    }
}
