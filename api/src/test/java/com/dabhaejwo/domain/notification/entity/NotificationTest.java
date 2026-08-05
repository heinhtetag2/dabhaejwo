package com.dabhaejwo.domain.notification.entity;

import com.dabhaejwo.global.security.OperatorRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 알림이 <b>누구에게 보이는가</b>가 이 도메인의 핵심 판단이다.
 * 여기서 새면 운영자가 남의 알림을 보거나, 받아야 할 사람이 못 받는다.
 */
class NotificationTest {

    @Test
    @DisplayName("대상 역할이 지정된 알림은 그 역할에게만 보인다")
    void restrictedToTargetRoles() {
        Notification notification = Notification.of(
                NotificationType.TENANT_COST_EXCEEDED, null, "제목", null, "/profitability", null);

        assertTrue(notification.visibleTo(OperatorRole.OPS_ADMIN));
        assertTrue(notification.visibleTo(OperatorRole.SALES));
        assertFalse(notification.visibleTo(OperatorRole.CS), "CS 에게 원가율 알림은 소음이다");
        assertFalse(notification.visibleTo(OperatorRole.DEV));
    }

    @Test
    @DisplayName("대상 역할이 비어 있으면 운영자 전원이 받는다")
    void emptyRolesMeansEveryone() {
        Notification notification = Notification.of(
                NotificationType.TENANT_SIGNED_UP, null, "가입", null, "/tenants", null);

        assertTrue(notification.targetRoleSet().isEmpty());
        for (OperatorRole role : OperatorRole.values()) {
            assertTrue(notification.visibleTo(role), role + " 도 가입 알림을 받아야 한다");
        }
    }

    @Test
    @DisplayName("업체 알림은 테넌트를 갖고 수신 대상이 TENANT 다")
    void tenantNotificationCarriesTenant() {
        UUID tenantId = UUID.randomUUID();
        Notification notification = Notification.of(
                NotificationType.IMPERSONATION_STARTED, tenantId,
                "운영팀이 접속했습니다", "사유: 문의 확인", "/app/team", null);

        assertEquals(NotificationAudience.TENANT, notification.getAudience());
        assertEquals(tenantId, notification.getTenantId());
    }

    @Test
    @DisplayName("두 번 읽어도 처음 읽은 시각이 남는다")
    void firstReadTimeWins() {
        Notification notification = Notification.of(
                NotificationType.LEAD_RECEIVED, UUID.randomUUID(),
                "새 연락처", null, "/app/leads", null);

        assertTrue(notification.unread());
        assertNull(notification.getReadAt());

        notification.markRead();
        var firstRead = notification.getReadAt();
        notification.markRead();

        assertFalse(notification.unread());
        assertEquals(firstRead, notification.getReadAt(), "읽은 시각을 덮어쓰면 안 된다");
    }

    @Test
    @DisplayName("종류마다 수신 대상이 하나로 정해져 있다")
    void audienceIsFixedPerType() {
        for (NotificationType type : NotificationType.values()) {
            if (type.audience() == NotificationAudience.TENANT) {
                assertTrue(type.roles().isEmpty(),
                        type + " 는 업체 알림이므로 역할로 나뉘지 않아야 한다");
            }
        }
    }
}
