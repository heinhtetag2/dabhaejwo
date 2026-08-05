package com.dabhaejwo.domain.notification.ws;

import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 알림 소켓.
 *
 * <p><b>토큰을 URL 에 싣지 않는다.</b> 브라우저 WebSocket API 는 커스텀 헤더를 못 붙이지만,
 * 쿼리 파라미터로 보내면 접근 로그·프록시 로그에 액세스 토큰이 그대로 남는다.
 * 그래서 연결한 뒤 <b>첫 프레임</b>으로 토큰을 받고, 그 전에는 어떤 알림도 보내지 않는다.
 *
 * <pre>
 * 클라이언트 → {"type":"AUTH","token":"eyJ..."}
 * 서버      → {"type":"READY"}            인증 성공
 * 서버      → {"type":"NOTIFICATION",...}  이후 알림
 * </pre>
 *
 * <p>인증에 실패하면 즉시 끊는다. 이유를 자세히 알려주지 않는다 — 토큰 종류를 떠보는
 * 수단이 되지 않게 한다.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    /** 인증 프레임이 오기 전 소켓이 붙어 있을 수 있는 시간. 넘으면 정리한다. */
    private static final long AUTH_TIMEOUT_MS = 10_000;

    /** 인증 프레임 하나면 충분하다. 큰 본문을 받을 이유가 없다. */
    private static final int MAX_FRAME_BYTES = 4 * 1024;

    private final JwtProvider jwtProvider;
    private final NotificationSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public NotificationWebSocketHandler(JwtProvider jwtProvider,
                                        NotificationSessionRegistry registry,
                                        ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(MAX_FRAME_BYTES);
        // 인증 없이 붙어만 있는 소켓을 방치하지 않는다.
        session.getAttributes().put("connectedAt", System.currentTimeMillis());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (registry.lookup(session) != null) {
            // 이미 인증된 소켓. 서버가 미는 한 방향이라 클라이언트 메시지를 받을 일이 없다.
            return;
        }

        Object connectedAt = session.getAttributes().get("connectedAt");
        if (connectedAt instanceof Long since
                && System.currentTimeMillis() - since > AUTH_TIMEOUT_MS) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        NotificationSession authenticated = authenticate(message.getPayload());
        if (authenticated == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        registry.register(session, authenticated);
        session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(Map.of("type", "READY"))));
    }

    /**
     * 첫 프레임 인증.
     *
     * <p>구독 대상은 <b>토큰에서만</b> 유도한다. 본문에 테넌트 id 를 받지 않는 것이 핵심이다 —
     * 받는 순간 남의 업체를 사칭해 알림을 훔쳐볼 수 있다(위젯 키를 본문에서 받지 않는 것과 같은 이유).
     */
    private NotificationSession authenticate(String payload) {
        try {
            JsonNode frame = objectMapper.readTree(payload);
            if (!"AUTH".equals(frame.path("type").asString())) {
                return null;
            }
            String token = frame.path("token").asString();
            if (token == null || token.isBlank()) {
                return null;
            }

            // 운영자 토큰인지 먼저 본다. 실패하면 업체 담당자 토큰으로 해석한다.
            // JwtProvider 가 typ 클레임으로 교차 사용을 막으므로 둘 다 실패하면 인증 불가다.
            try {
                AuthPrincipal.Operator operator = jwtProvider.parseOperator(token);
                return NotificationSession.forOperator(operator.role());
            } catch (RuntimeException ignored) {
                AuthPrincipal.TenantUser user = jwtProvider.parseTenantUser(token);
                return NotificationSession.forTenant(user.tenantId(), user.impersonating());
            }
        } catch (RuntimeException e) {
            log.debug("소켓 인증 실패");
            return null;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        registry.remove(session);
    }
}
