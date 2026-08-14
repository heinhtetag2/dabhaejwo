package com.dabhaejwo.domain.bot.entity;

import com.dabhaejwo.global.common.HostName;
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
 * 서비스 — 챗봇 한 벌.
 *
 * <p><b>화면에서는 "서비스"라고 부른다.</b> 코드가 {@code bot} 인 이유는
 * {@code domain/{도메인}/service/} 가 레이어 관례라, 도메인을 {@code service} 로 만들면
 * {@code domain/service/service/ServiceService} 가 나오기 때문이다.
 * 이 프로젝트는 원래 화면 용어와 스키마 용어가 다르다({@code leads} → "남긴 연락처",
 * {@code answer_gaps} → "답변 개선").
 *
 * <p>이 단위로 갈리는 것 — 위젯 키 · 봇 설정 · 지식 · 공통 질문 · 대화 · 연락처 · 허용 주소.
 * 업체 하나로 남는 것 — 계정 · 팀원 · 요금제 · 결제 · <b>한도</b>.
 *
 * <p>기획: {@code docs/plan/service-plan.md}
 */
@Entity
@Table(name = "bots")
public class Bot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "primary_domain", nullable = false)
    private String primaryDomain;

    /**
     * 위젯 키. <b>남의 사이트 소스에 박히므로 재발급이 사실상 불가능하다</b> —
     * 우리는 그 {@code <script>} 태그를 고칠 수 없다.
     */
    @Column(name = "publishable_key", nullable = false)
    private String publishableKey;

    /**
     * 업체가 이 서비스를 켜뒀는가. {@link com.dabhaejwo.domain.tenant.entity.TenantStatus}
     * (계약: 돈을 내는가)와 <b>다른 축</b>이라 위젯 인증은 둘 다 본다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStatus status;

    /** 서비스를 지목하지 않는 옛 경로(`/app/leads`)와 백필의 착지점. 업체당 정확히 하나다. */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "purge_after")
    private OffsetDateTime purgeAfter;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Bot() {
    }

    public static Bot of(UUID tenantId, String name, String primaryDomain, String publishableKey,
                         boolean isDefault) {
        Bot bot = new Bot();
        bot.tenantId = tenantId;
        bot.name = name.strip();
        bot.primaryDomain = HostName.normalize(primaryDomain);
        bot.publishableKey = publishableKey;
        bot.status = BotStatus.ACTIVE;
        bot.isDefault = isDefault;
        bot.createdAt = OffsetDateTime.now();
        bot.updatedAt = bot.createdAt;
        return bot;
    }

    /**
     * 방문자에게 이 서비스를 띄워도 되는가.
     *
     * <p>업체 계약 상태는 <b>여기서 보지 않는다</b> — 호출부가 함께 확인한다.
     * 두 축을 한 곳에 뭉치면 "왜 안 뜨는지"가 계약 문제인지 설정 문제인지 구분되지 않는다.
     */
    public boolean servesVisitors() {
        return status == BotStatus.ACTIVE;
    }

    /** {@code tenantId} 와 {@code id} 를 절대 따로 넘기지 않기 위한 값이다. */
    public BotScope scope() {
        return new BotScope(tenantId, id);
    }

    /**
     * 삭제 예약.
     *
     * <p><b>위젯은 즉시 멈추고 데이터는 유예 기간 뒤에 지운다.</b> 바로 지우면 실수로 누른
     * 업체를 되살릴 수 없다 — 지식·대화·연락처가 함께 사라지는 행위다.
     */
    public void scheduleDeletion(int graceDays) {
        this.status = BotStatus.DELETING;
        this.deletedAt = OffsetDateTime.now();
        this.purgeAfter = this.deletedAt.plusDays(graceDays);
        touch();
    }

    /** 유예 중 되돌리기. 아직 아무것도 지워지지 않았으므로 그대로 살아난다. */
    public void restore() {
        this.status = BotStatus.ACTIVE;
        this.deletedAt = null;
        this.purgeAfter = null;
        touch();
    }

    /** 유예가 끝났는가. 지날 때까지는 되돌릴 수 있다. */
    public boolean purgeDue(OffsetDateTime now) {
        return status == BotStatus.DELETING && purgeAfter != null && !purgeAfter.isAfter(now);
    }

    /**
     * 기본 서비스 자리를 넘긴다.
     *
     * <p>기본 서비스를 지우려면 먼저 다른 것이 그 자리를 받아야 한다 — 옛 경로
     * ({@code /app/leads})와 대리 접속이 착지할 곳이 없어지면 안 된다.
     */
    public void makeDefault(boolean value) {
        this.isDefault = value;
        touch();
    }

    public void rename(String name, String primaryDomain) {
        this.name = name.strip();
        this.primaryDomain = HostName.normalize(primaryDomain);
        touch();
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getPrimaryDomain() {
        return primaryDomain;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public BotStatus getStatus() {
        return status;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public OffsetDateTime getPurgeAfter() {
        return purgeAfter;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
