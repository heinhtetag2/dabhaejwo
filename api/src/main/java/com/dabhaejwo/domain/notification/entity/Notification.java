package com.dabhaejwo.domain.notification.entity;

import com.dabhaejwo.global.security.OperatorRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 알림 한 건.
 *
 * <p>WebSocket 은 전달 채널일 뿐 <b>진실은 이 행이다.</b> 접속 중이 아닐 때 발생한 알림도
 * 여기 남아 다음 접속 때 보인다 — 한도 초과나 대리 접속처럼 놓치면 안 되는 것들이다.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationAudience audience;

    /** 업체 알림이면 채워진다. 운영자 알림은 null. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "operator_id")
    private UUID operatorId;

    /**
     * 받을 역할. 비어 있으면 전 역할.
     *
     * <p>{@code text[]} 이며 조회 조건으로 쓰지 않는다 — 목록을 읽어 메모리에서 거른다.
     * 운영자 알림은 건수가 작고, 배열을 조건으로 쓰기 시작하면 조인 테이블이 필요해진다.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "target_roles", nullable = false, columnDefinition = "text[]")
    private String[] targetRoles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    private String body;

    /** 누르면 바로 그 대상으로 간다. 이름만 확인하고 다시 찾게 만들지 않는다. */
    @Column(name = "target_path")
    private String targetPath;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** 중복 억제 키. 같은 키는 DB 유니크 인덱스가 한 번만 허용한다. */
    @Column(name = "dedupe_key")
    private String dedupeKey;

    protected Notification() {
    }

    public static Notification of(NotificationType type,
                                  UUID tenantId,
                                  String title,
                                  String body,
                                  String targetPath,
                                  String dedupeKey) {
        Notification notification = new Notification();
        notification.type = type;
        notification.audience = type.audience();
        notification.tenantId = tenantId;
        notification.targetRoles = type.roles().stream().map(Enum::name).toArray(String[]::new);
        notification.title = title;
        notification.body = body;
        notification.targetPath = targetPath;
        notification.dedupeKey = dedupeKey;
        notification.createdAt = OffsetDateTime.now();
        return notification;
    }

    /**
     * 이 역할이 받을 알림인가. 대상 역할이 비어 있으면 전원이 받는다.
     */
    public boolean visibleTo(OperatorRole role) {
        if (targetRoles == null || targetRoles.length == 0) {
            return true;
        }
        return Arrays.asList(targetRoles).contains(role.name());
    }

    /** 이미 읽었으면 시각을 덮지 않는다 — 처음 읽은 때가 진실이다. */
    public void markRead() {
        if (readAt == null) {
            readAt = OffsetDateTime.now();
        }
    }

    public boolean unread() {
        return readAt == null;
    }

    public Long getId() {
        return id;
    }

    public NotificationAudience getAudience() {
        return audience;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<String> getTargetRoles() {
        return targetRoles == null ? List.of() : List.of(targetRoles);
    }

    /** 테스트·발행 경로에서 역할 집합을 확인할 때. */
    public Set<String> targetRoleSet() {
        return Set.copyOf(getTargetRoles());
    }
}
