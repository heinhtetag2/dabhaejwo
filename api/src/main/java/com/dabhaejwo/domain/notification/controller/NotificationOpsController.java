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
 * 운영자 알림.
 *
 * <p>권한을 걸지 않는다 — 알림은 <b>역할로 이미 걸러져</b> 내려간다. 자기 역할이 받을
 * 알림만 보이므로 별도 권한 키를 두면 두 곳에서 같은 판단을 하게 된다.
 */
@RestController
@RequestMapping("/api/ops/notifications")
public class NotificationOpsController {

    private final NotificationQueryService service;

    public NotificationOpsController(NotificationQueryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(required = false) Integer size) {
        return service.listForOps(page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", service.unreadCountForOps());
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long notificationId) {
        service.markReadForOps(notificationId);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", service.markAllReadForOps());
    }
}
