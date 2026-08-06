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

    /**
     * 방문자에게 위젯을 띄울지.
     *
     * <p>끄면 위젯이 <b>마운트 자체를 포기한다</b> — 오류를 그리지 않는다. 허용 주소를 지워
     * 막던 우회로와 다른 점이 이것이다. 그쪽은 방문자에게 "답변을 드리기 어렵습니다"가 떠서
     * 꺼진 게 아니라 고장 난 것처럼 보인다.
     */
    @Column(name = "widget_enabled", nullable = false)
    private boolean widgetEnabled;

    /**
     * 런처에 넣을 이미지. 없으면 {@link #logoUrl}, 그것도 없으면 기본 말풍선 아이콘.
     *
     * <p>우리 API 를 가리키는 경로다({@code /api/public/assets/...}). 외부 URL 을 받지 않는 이유는
     * 업체가 남의 서버 이미지를 걸면 그 서버가 죽었을 때 <b>우리 위젯이 깨져 보이기</b> 때문이다.
     */
    @Column(name = "launcher_icon_url")
    private String launcherIconUrl;

    /** 업체 로고. 콘솔 사이드바에 뜨고, 런처 아이콘이 없으면 런처에도 쓰인다. */
    @Column(name = "logo_url")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "launcher_size", nullable = false)
    private LauncherSize launcherSize;

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
        settings.widgetEnabled = true;
        settings.launcherSize = LauncherSize.MEDIUM;
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
                              int nudgeDelaySeconds,
                              boolean widgetEnabled,
                              LauncherSize launcherSize) {
        if (nudgeDelaySeconds < 0) {
            throw new IllegalArgumentException("nudgeDelaySeconds must be >= 0");
        }
        this.widgetEnabled = widgetEnabled;
        this.launcherSize = launcherSize == null ? LauncherSize.MEDIUM : launcherSize;
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

    public boolean isWidgetEnabled() {
        return widgetEnabled;
    }

    public String getLauncherIconUrl() {
        return launcherIconUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public LauncherSize getLauncherSize() {
        return launcherSize;
    }

    /**
     * 런처에 실제로 그릴 이미지. <b>아이콘 > 로고 > 없음</b> 순이다.
     *
     * <p>둘 다 올린 업체에게 무엇이 이기는지를 화면이 아니라 여기서 정한다 —
     * 화면마다 다르게 고르면 미리보기와 실제 위젯이 어긋난다.
     */
    public String effectiveLauncherImageUrl() {
        if (launcherIconUrl != null && !launcherIconUrl.isBlank()) {
            return launcherIconUrl;
        }
        return logoUrl == null || logoUrl.isBlank() ? null : logoUrl;
    }

    /** 업로드한 이미지 경로를 바꾼다. null 을 주면 지운다(기본 아이콘으로 돌아간다). */
    public void changeBranding(String launcherIconUrl, String logoUrl) {
        this.launcherIconUrl = launcherIconUrl;
        this.logoUrl = logoUrl;
        touch();
    }
}
