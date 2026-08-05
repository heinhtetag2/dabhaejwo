package com.dabhaejwo.global.security;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 요청자 IP.
 *
 * <p>가입 제한·OTP 재발송 제한·방문자 질문 제한이 같은 계산을 세 벌 갖고 있었다.
 * 한쪽만 고치면 어긋나므로 여기 하나만 둔다.
 *
 * <p><b>이 값은 신뢰 경계가 아니다.</b> 레이트 리밋 키로만 쓰고 인가 판단에는 쓰지 않는다.
 */
public final class ClientIp {

    private ClientIp() {
    }

    /**
     * {@code X-Forwarded-For} 의 <b>마지막</b> 항목을 쓴다.
     *
     * <p>처음엔 첫 항목을 썼는데, 프록시 뒤에 놓는 순간 그게 구멍이 된다 —
     * 리버스 프록시는 들어온 헤더에 실제 IP 를 <b>덧붙인다.</b> 클라이언트가
     * {@code X-Forwarded-For: 1.2.3.4} 를 달고 오면 앱은 {@code "1.2.3.4, <진짜>"} 를 받고,
     * 첫 항목을 고르면 <b>위조값을 그대로 믿는다.</b> 헤더를 매번 바꿔가며 보내면
     * 가입 제한·OTP 재발송 제한·방문자 질문 제한이 전부 무력화된다.
     *
     * <p>마지막 항목은 <b>우리 프록시가 직접 적은 값</b>이라 클라이언트가 손댈 수 없다.
     * 프록시가 없는 개발 환경에서는 헤더 자체가 없어 {@code getRemoteAddr()} 로 떨어진다.
     *
     * <p>프록시를 두 단 이상 두게 되면(CDN → Apache) 이 규칙을 다시 봐야 한다 —
     * 그때는 마지막이 CDN 이 아니라 앞단 프록시의 IP 다.
     */
    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            String last = hops[hops.length - 1].strip();
            if (!last.isEmpty()) {
                return last;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 해시. <b>원본 IP 를 저장하지 않는다</b> — 개인정보이고, 우리가 필요한 것은
     * "같은 사람인가"뿐이라 해시로 충분하다.
     */
    public static String hashOf(HttpServletRequest request) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(of(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }
}
