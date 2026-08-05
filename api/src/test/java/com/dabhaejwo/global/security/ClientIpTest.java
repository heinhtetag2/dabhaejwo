package com.dabhaejwo.global.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 요청자 IP 판정.
 *
 * <p>이 값은 가입 제한·OTP 재발송 제한·방문자 질문 제한의 <b>키</b>다. 여기가 클라이언트
 * 마음대로 바뀌면 세 제한이 한꺼번에 무력화되므로, 프록시 뒤에 놓는 순간 규칙이 중요해진다.
 */
class ClientIpTest {

    @Test
    @DisplayName("헤더가 없으면 연결 IP 를 쓴다")
    void fallsBackToRemoteAddr() {
        assertEquals("203.0.113.9", ClientIp.of(request(null, "203.0.113.9")));
    }

    @Test
    @DisplayName("프록시 한 단이면 그 프록시가 적은 IP 를 쓴다")
    void singleProxy() {
        assertEquals("203.0.113.9", ClientIp.of(request("203.0.113.9", "127.0.0.1")));
    }

    @Test
    @DisplayName("클라이언트가 X-Forwarded-For 를 위조해도 속지 않는다")
    void ignoresSpoofedPrefix() {
        // 프록시는 들어온 헤더를 지우지 않고 실제 IP 를 뒤에 덧붙인다.
        // 첫 항목을 고르면 위조값("1.2.3.4")을 그대로 믿게 된다 — 과거 구현이 그랬다.
        String forwarded = "1.2.3.4, 203.0.113.9";

        assertEquals("203.0.113.9", ClientIp.of(request(forwarded, "127.0.0.1")));
        assertNotEquals("1.2.3.4", ClientIp.of(request(forwarded, "127.0.0.1")));
    }

    @Test
    @DisplayName("위조값을 바꿔가며 보내도 같은 사람으로 세어진다")
    void spoofingDoesNotChangeTheKey() {
        // 레이트 리밋이 실제로 지켜지는지는 이 성질에 달려 있다 —
        // 헤더만 바꿔 매번 다른 키가 나오면 한도가 의미를 잃는다.
        String first = ClientIp.hashOf(request("9.9.9.9, 203.0.113.9", "127.0.0.1"));
        String second = ClientIp.hashOf(request("8.8.8.8, 203.0.113.9", "127.0.0.1"));

        assertEquals(first, second);
    }

    @Test
    @DisplayName("해시는 원본 IP 를 담지 않는다")
    void hashHidesTheAddress() {
        String hash = ClientIp.hashOf(request(null, "203.0.113.9"));

        assertEquals(32, hash.length());
        assertNotEquals("203.0.113.9", hash);
    }

    /** 서블릿 요청은 이 두 값만 쓴다. 목 프레임워크를 들이지 않고 프록시로 만든다. */
    private HttpServletRequest request(String forwardedFor, String remoteAddr) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getHeader" -> "X-Forwarded-For".equals(args[0]) ? forwardedFor : null;
            case "getRemoteAddr" -> remoteAddr;
            default -> null;
        };
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                handler);
    }
}
