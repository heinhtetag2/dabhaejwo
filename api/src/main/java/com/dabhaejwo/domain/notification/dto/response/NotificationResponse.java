package com.dabhaejwo.domain.notification.dto.response;

import com.dabhaejwo.domain.notification.entity.Notification;
import com.dabhaejwo.domain.notification.entity.NotificationType;

import java.time.OffsetDateTime;

/**
 * 알림 한 건. api-contracts.md §11.
 *
 * <p>{@code targetPath} 는 프론트 라우트다. 누르면 바로 그 대상으로 간다.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        NotificationType.Severity severity,
        String title,
        String body,
        String targetPath,
        boolean read,
        OffsetDateTime createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getType().severity(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetPath(),
                !notification.unread(),
                notification.getCreatedAt());
    }
}
