package com.dabhaejwo.domain.notification.service;

import com.dabhaejwo.domain.notification.dto.response.NotificationResponse;
import com.dabhaejwo.domain.notification.entity.Notification;
import com.dabhaejwo.domain.notification.entity.NotificationAudience;
import com.dabhaejwo.domain.notification.repository.NotificationRepository;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 알림 조회·읽음 처리.
 *
 * <p>수신자는 <b>토큰에서만</b> 유도한다. 파라미터로 받으면 남의 알림을 읽을 수 있다.
 */
@Service
public class NotificationQueryService {

    private final NotificationRepository repository;

    public NotificationQueryService(NotificationRepository repository) {
        this.repository = repository;
    }

    // ── 업체 ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForTenant(int page, Integer size) {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();
        return PageResponse.of(
                repository.findByAudienceAndTenantIdOrderByCreatedAtDesc(
                        NotificationAudience.TENANT, user.tenantId(),
                        PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size))),
                NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCountForTenant() {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();
        return repository.findByAudienceAndTenantIdAndReadAtIsNull(
                NotificationAudience.TENANT, user.tenantId()).size();
    }

    /**
     * 업체 알림 읽음 처리.
     *
     * <p><b>대리 접속 세션은 하지 못한다.</b> 운영자가 업체 알림을 읽음으로 만들면 업체는
     * 영영 못 본다 — 특히 "운영팀이 접속했습니다" 알림을 그 운영자가 지우는 셈이 된다.
     */
    @Transactional
    public void markReadForTenant(Long notificationId) {
        AuthPrincipal.TenantUser user = requireNotImpersonating();
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 테넌트 격리 — 남의 알림은 존재 여부도 알려주지 않는다.
        if (notification.getAudience() != NotificationAudience.TENANT
                || !user.tenantId().equals(notification.getTenantId())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notification.markRead();
    }

    @Transactional
    public int markAllReadForTenant() {
        AuthPrincipal.TenantUser user = requireNotImpersonating();
        return repository.markAllRead(
                NotificationAudience.TENANT, user.tenantId(), OffsetDateTime.now());
    }

    private AuthPrincipal.TenantUser requireNotImpersonating() {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();
        if (user.impersonating()) {
            throw new BusinessException(ErrorCode.IMPERSONATION_FORBIDDEN_ACTION,
                    "대리 접속 중에는 업체의 알림을 읽음 처리할 수 없습니다");
        }
        return user;
    }

    // ── 운영자 ──────────────────────────────────────────────

    /**
     * 운영자 알림. 역할 필터는 메모리에서 한다 — 대상 역할이 배열이라 조회 조건으로 쓰면
     * 조인 테이블이 필요해지고, 운영자 알림은 건수가 작다.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForOps(int page, Integer size) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        int pageSize = PageResponse.clampSize(size);
        int pageNumber = Math.max(page, 0);

        List<NotificationResponse> visible = repository
                .findByAudienceOrderByCreatedAtDesc(
                        NotificationAudience.OPS,
                        // 역할로 거르면 개수가 줄어드므로 넉넉히 읽어 온 뒤 자른다.
                        PageRequest.of(0, (pageNumber + 1) * pageSize * 3))
                .getContent().stream()
                .filter(notification -> notification.visibleTo(operator.role()))
                .map(NotificationResponse::from)
                .toList();

        int from = Math.min(pageNumber * pageSize, visible.size());
        int to = Math.min(from + pageSize, visible.size());
        return new PageResponse<>(
                List.copyOf(visible.subList(from, to)),
                new PageResponse.PageInfo(pageNumber, pageSize, visible.size(),
                        (int) Math.ceil((double) visible.size() / pageSize)));
    }

    @Transactional(readOnly = true)
    public long unreadCountForOps() {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        return repository.findByAudienceAndReadAtIsNull(NotificationAudience.OPS).stream()
                .filter(notification -> notification.visibleTo(operator.role()))
                .count();
    }

    @Transactional
    public void markReadForOps(Long notificationId) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.getAudience() != NotificationAudience.OPS
                || !notification.visibleTo(operator.role())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notification.markRead();
    }

    /**
     * 운영자 전체 읽음.
     *
     * <p>역할이 못 보는 알림까지 읽음이 되지만, 어차피 그 운영자에게는 보이지 않으므로
     * 화면상 차이가 없다. 알림은 개인이 아니라 역할 단위로 공유되는 성격이다.
     */
    @Transactional
    public int markAllReadForOps() {
        CurrentAuth.operator();
        return repository.markAllRead(NotificationAudience.OPS, null, OffsetDateTime.now());
    }
}
