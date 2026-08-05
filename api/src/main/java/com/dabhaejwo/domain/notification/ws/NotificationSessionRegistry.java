package com.dabhaejwo.domain.notification.ws;

import com.dabhaejwo.domain.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 지금 붙어 있는 소켓들.
 *
 * <p><b>인스턴스 메모리에만 있다.</b> 서버가 여러 대가 되면 A 인스턴스에 붙은 사용자는
 * B 에서 발생한 알림을 실시간으로 못 받는다 — 다만 알림 자체는 DB 에 남아 목록에는
 * 보이므로 잃지는 않는다. 레이트 리밋·스케줄러와 같은 종류의 한계다
 * (docs/IMPROVEMENTS.md).
 */
@Component
public class NotificationSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(NotificationSessionRegistry.class);

    /** 소켓 id → 인증 정보. 인증 전에는 등록되지 않는다. */
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession socket, NotificationSession authenticated) {
        sessions.put(socket.getId(), new Entry(socket, authenticated));
    }

    public void remove(WebSocketSession socket) {
        sessions.remove(socket.getId());
    }

    /** 인증된 세션인가. 인증 전 소켓은 어떤 메시지도 받지 못한다. */
    public NotificationSession lookup(WebSocketSession socket) {
        Entry entry = sessions.get(socket.getId());
        return entry == null ? null : entry.session();
    }

    /**
     * 자격이 있는 소켓에만 보낸다.
     *
     * <p>보내다 실패한 소켓은 조용히 지운다 — 이미 끊긴 연결이며, 알림은 DB 에 남아 있다.
     */
    public void push(Notification notification) {
        String payload = objectMapper.writeValueAsString(Map.of(
                "type", "NOTIFICATION",
                "notification", Map.of(
                        "id", notification.getId(),
                        "type", notification.getType().name(),
                        "severity", notification.getType().severity().name(),
                        "title", notification.getTitle(),
                        "body", notification.getBody() == null ? "" : notification.getBody(),
                        "targetPath", notification.getTargetPath() == null ? "" : notification.getTargetPath(),
                        // 방금 만들어진 알림이라 항상 안 읽음이다. 그래도 내려주는 이유는
                        // REST 목록과 <b>같은 모양</b>이어야 클라이언트가 변환 없이 합칠 수 있어서다.
                        "read", false,
                        "createdAt", notification.getCreatedAt().toString())));

        sessions.values().stream()
                .filter(entry -> entry.session().accepts(notification))
                .forEach(entry -> send(entry, payload));
    }

    private void send(Entry entry, String payload) {
        try {
            synchronized (entry.socket()) {
                // WebSocketSession 은 동시 전송에 안전하지 않다. 두 알림이 동시에 나가면
                // 프레임이 섞여 클라이언트가 파싱에 실패한다.
                if (entry.socket().isOpen()) {
                    entry.socket().sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException | RuntimeException e) {
            log.debug("소켓 전송 실패 — 정리합니다. id={}", entry.socket().getId());
            sessions.remove(entry.socket().getId());
        }
    }

    private record Entry(WebSocketSession socket, NotificationSession session) {
    }
}
