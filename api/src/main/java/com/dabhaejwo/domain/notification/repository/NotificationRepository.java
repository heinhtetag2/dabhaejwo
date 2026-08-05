package com.dabhaejwo.domain.notification.repository;

import com.dabhaejwo.domain.notification.entity.Notification;
import com.dabhaejwo.domain.notification.entity.NotificationAudience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 업체 알림. <b>테넌트 조건이 항상 붙는다</b> — 남의 알림이 섞이면 P0 다.
     */
    Page<Notification> findByAudienceAndTenantIdOrderByCreatedAtDesc(
            NotificationAudience audience, UUID tenantId, Pageable pageable);

    /** 운영자 알림. 역할 필터는 메모리에서 한다(건수가 작고 배열 조건을 쓰지 않는다). */
    Page<Notification> findByAudienceOrderByCreatedAtDesc(
            NotificationAudience audience, Pageable pageable);

    List<Notification> findByAudienceAndTenantIdAndReadAtIsNull(
            NotificationAudience audience, UUID tenantId);

    List<Notification> findByAudienceAndReadAtIsNull(NotificationAudience audience);

    boolean existsByDedupeKey(String dedupeKey);

    /**
     * 모두 읽음. 한 건씩 불러 고치면 알림이 쌓인 계정에서 느리다.
     *
     * <p>업체 알림은 {@code tenantId} 로 좁힌다 — null 이면 운영자 알림 전체다.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n SET n.readAt = :now
            WHERE n.audience = :audience
              AND (:tenantId IS NULL OR n.tenantId = :tenantId)
              AND n.readAt IS NULL
            """)
    int markAllRead(@Param("audience") NotificationAudience audience,
                    @Param("tenantId") UUID tenantId,
                    @Param("now") OffsetDateTime now);
}
