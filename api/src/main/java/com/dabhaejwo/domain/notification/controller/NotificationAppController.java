package com.dabhaejwo.domain.notification.controller;

import com.dabhaejwo.domain.notification.dto.response.NotificationResponse;
import com.dabhaejwo.domain.notification.service.NotificationQueryService;
import com.dabhaejwo.global.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 업체 알림. 수신자는 토큰에서 유도하므로 <b>테넌트를 파라미터로 받지 않는다.</b>
 */
@RestController
@RequestMapping("/api/app/notifications")
public class NotificationAppController {

    private final NotificationQueryService service;

    public NotificationAppController(NotificationQueryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(required = false) Integer size) {
        return service.listForTenant(page, size);
    }

    /** 벨 배지. 폴링 없이 소켓으로도 갱신되지만 첫 진입에는 이 값이 필요하다. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCountForTenant());
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long notificationId) {
        service.markReadForTenant(notificationId);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", service.markAllReadForTenant());
    }
}
