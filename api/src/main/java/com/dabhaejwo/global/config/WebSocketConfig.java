package com.dabhaejwo.global.config;

import com.dabhaejwo.domain.notification.ws.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * 알림 소켓 경로.
 *
 * <p>핸드셰이크는 시큐리티에서 열어둔다({@code SecurityConfig}) — <b>인증을 안 한다는 뜻이 아니라</b>
 * 브라우저가 핸드셰이크에 Authorization 헤더를 실을 수 없어 필터가 할 일이 없다는 뜻이다.
 * 인증은 연결 직후 첫 프레임에서 핸들러가 한다({@code NotificationWebSocketHandler}).
 *
 * <p>Origin 은 여기서 제한한다. 기본값은 전체 허용이라 아무 사이트나 소켓을 열 수 있다 —
 * 토큰이 없으면 아무것도 못 받지만, 굳이 문을 열어둘 이유가 없다.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;
    private final List<String> allowedOrigins;

    public WebSocketConfig(NotificationWebSocketHandler handler, AppProperties properties) {
        this.handler = handler;
        this.allowedOrigins = properties.cors().allowedOrigins();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
    }
}
