package com.dabhaejwo.domain.notification.ws;

import com.dabhaejwo.domain.notification.entity.Notification;
import com.dabhaejwo.domain.notification.entity.NotificationAudience;
import com.dabhaejwo.global.security.OperatorRole;

import java.util.UUID;

/**
 * 인증이 끝난 소켓 하나가 무엇을 받을 자격이 있는지.
 *
 * <p><b>전부 서버가 토큰에서 유도한 값이다.</b> 클라이언트가 보낸 어떤 값도 여기 들어오지
 * 않는다 — "나는 업체 X요"라고 주장할 수 있으면 남의 알림을 받게 되고, 그건 P0 다.
 *
 * @param tenantId    업체 담당자 세션이면 채워진다. 운영자 세션은 null
 * @param role        운영자 세션이면 채워진다. 역할별 알림을 거르는 데 쓴다
 * @param impersonating 대리 접속 세션인가. 이 세션은 <b>읽음 처리를 하지 못한다</b> —
 *                    운영자가 업체 알림을 읽음으로 만들면 업체가 영영 못 본다
 */
public record NotificationSession(NotificationAudience audience,
                                  UUID tenantId,
                                  OperatorRole role,
                                  boolean impersonating) {

    public static NotificationSession forTenant(UUID tenantId, boolean impersonating) {
        return new NotificationSession(NotificationAudience.TENANT, tenantId, null, impersonating);
    }

    public static NotificationSession forOperator(OperatorRole role) {
        return new NotificationSession(NotificationAudience.OPS, null, role, false);
    }

    /** 이 알림을 이 세션이 받아도 되는가. */
    public boolean accepts(Notification notification) {
        if (notification.getAudience() != audience) {
            return false;
        }
        if (audience == NotificationAudience.TENANT) {
            return tenantId != null && tenantId.equals(notification.getTenantId());
        }
        return role != null && notification.visibleTo(role);
    }
}
