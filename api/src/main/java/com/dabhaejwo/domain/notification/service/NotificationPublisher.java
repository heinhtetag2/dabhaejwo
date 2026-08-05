package com.dabhaejwo.domain.notification.service;

import com.dabhaejwo.domain.notification.entity.Notification;
import com.dabhaejwo.domain.notification.entity.NotificationType;
import com.dabhaejwo.domain.notification.repository.NotificationRepository;
import com.dabhaejwo.domain.notification.ws.NotificationSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * 알림 발행의 <b>유일한 경로.</b>
 *
 * <p>저장 → 중복 억제 → 실시간 전달이 한 곳에 있다. 흩어 두면 어떤 알림은 저장만 되고
 * 어떤 알림은 푸시만 되는 상태가 생긴다.
 *
 * <p>WebSocket 전달은 <b>실패해도 무시한다.</b> 저장이 진실이고 푸시는 편의다 —
 * 소켓이 끊겼다고 알림 자체를 잃으면 안 된다.
 */
@Service
public class NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

    private final NotificationRepository repository;
    private final NotificationSessionRegistry registry;

    public NotificationPublisher(NotificationRepository repository,
                                 NotificationSessionRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    /**
     * 호출한 트랜잭션에 참여한다.
     *
     * <p>업무가 롤백되면 알림도 사라져야 하는 경우에 쓴다 — 가입이 실패했는데
     * "가입했습니다" 알림이 남으면 운영자가 없는 업체를 찾아다닌다.
     */
    @Transactional
    public void publish(NotificationType type, UUID tenantId,
                        String title, String body, String targetPath, String dedupeKey) {
        save(type, tenantId, title, body, targetPath, dedupeKey);
    }

    /**
     * 별도 트랜잭션으로 남긴다.
     *
     * <p><b>호출부가 예외를 던져 롤백하는 경우</b>에만 쓴다. 대표적으로 일일 원가 상한 —
     * 한도에 걸린 요청은 {@code COST_CAP_REACHED} 로 거절되며 트랜잭션이 되감긴다.
     * 같은 트랜잭션에 있으면 "상한에 도달했다"는 사실까지 함께 사라져 아무도 모르게 된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishDetached(NotificationType type, UUID tenantId,
                                String title, String body, String targetPath, String dedupeKey) {
        save(type, tenantId, title, body, targetPath, dedupeKey);
    }

    private void save(NotificationType type, UUID tenantId,
                      String title, String body, String targetPath, String dedupeKey) {
        // 원가 상한 도달은 요청마다 발생한다. 이 검사가 없으면 알림창이 같은 문장으로 도배된다.
        if (dedupeKey != null && repository.existsByDedupeKey(dedupeKey)) {
            return;
        }

        Notification saved;
        try {
            saved = repository.saveAndFlush(
                    Notification.of(type, tenantId, title, body, targetPath, dedupeKey));
        } catch (DataIntegrityViolationException e) {
            // 위 검사와 저장 사이에 다른 요청이 먼저 넣었다. 유니크 인덱스가 잡아준 것이므로
            // 중복을 만들지 않은 정상 동작이다.
            log.debug("중복 알림을 건너뜁니다 — key={}", dedupeKey);
            return;
        }

        pushAfterCommit(saved);
    }

    /**
     * 실시간 전달은 <b>커밋 뒤에</b> 한다.
     *
     * <p>여기서 바로 밀면 이후 롤백된 트랜잭션의 알림이 화면에 떠 있게 된다 — 사용자는
     * 눌러서 없는 대상으로 이동하고, 새로고침하면 알림이 사라진다. 되돌릴 수 없는 동작은
     * 되돌릴 수 있는 것들이 다 끝난 뒤에 한다.
     */
    private void pushAfterCommit(Notification saved) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deliver(saved);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliver(saved);
            }
        });
    }

    private void deliver(Notification saved) {
        try {
            registry.push(saved);
        } catch (RuntimeException e) {
            // 전달 실패는 알림 자체의 실패가 아니다. 다음 접속 때 목록으로 보인다.
            log.warn("알림 실시간 전달에 실패했습니다 — id={}", saved.getId(), e);
        }
    }
}
