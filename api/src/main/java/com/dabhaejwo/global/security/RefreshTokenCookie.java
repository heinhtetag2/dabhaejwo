package com.dabhaejwo.global.security;

import com.dabhaejwo.global.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 리프레시 토큰을 담는 쿠키.
 *
 * <p><b>왜 쿠키인가.</b> 액세스 토큰은 메모리에만 둔다 — {@code localStorage} 에 두면 XSS
 * 한 번에 통째로 털린다. 그런데 리프레시 토큰까지 메모리에 두면 <b>새로고침할 때마다
 * 로그아웃된다.</b> 탭을 닫았다 열거나 F5 한 번에 다시 로그인해야 하는 것은 제품이 아니다.
 *
 * <p>답은 브라우저가 들고 있되 <b>자바스크립트가 못 읽는 곳</b>에 두는 것이다. {@code httpOnly}
 * 쿠키는 XSS 로 값을 빼갈 수 없고, 새로고침해도 살아 있다. 이 파일이 그 규약을 혼자 안다 —
 * 이름·경로·수명이 흩어지면 어느 한 곳만 고쳐져 로그아웃이 조용히 되돌아온다.
 *
 * <p><b>경로를 {@code /api/auth} 로 좁힌다.</b> 이 쿠키가 필요한 곳은 재발급과 로그아웃뿐인데,
 * 경로를 {@code /} 로 두면 모든 API 호출에 리프레시 토큰이 따라붙는다. 쓰이지 않는 곳까지
 * 실어 보내면 로그·프록시·오류 리포트에 남을 자리만 늘어난다.
 *
 * <p><b>SameSite 는 {@code Lax} 다.</b> 콘솔과 API 가 다른 <i>호스트</i>지만 같은
 * <i>사이트</i>(등록가능 도메인)라 쿠키가 정상적으로 실린다 — 예: {@code dabhaejwo-api.tagoplus.co.kr}
 * 와 {@code dabhaejwo-mng.tagoplus.co.kr} 은 둘 다 {@code tagoplus.co.kr} 이고, 개발에서는
 * 둘 다 {@code localhost} 다. <b>API 를 다른 등록가능 도메인으로 옮기면 이 값을 {@code None}
 * 으로 바꿔야 하고, 그러면 {@code Secure} 가 필수가 된다.</b> 그래서 설정으로 뺐다.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "dabhaejwo_refresh";

    /** 재발급과 로그아웃에만 필요하다. 다른 요청에는 실리지 않는다. */
    private static final String PATH = "/api/auth";

    private final AppProperties.Auth auth;

    public RefreshTokenCookie(AppProperties properties) {
        this.auth = properties.auth();
    }

    /** 로그인·가입·재발급 성공 시. 수명은 리프레시 토큰 자체의 만료와 같게 맞춘다. */
    public void issue(HttpServletResponse response, String refreshToken) {
        response.addHeader("Set-Cookie", build(refreshToken,
                Duration.ofDays(auth.refreshTtlDays())).toString());
    }

    /**
     * 로그아웃.
     *
     * <p>지우지 않으면 화면에서 로그아웃해도 새로고침 한 번에 <b>다시 로그인된 상태로 돌아온다</b> —
     * 쿠키가 살아 있으니 부트스트랩이 조용히 재발급을 받아낸다. 공용 PC 에서는 사고다.
     */
    public void clear(HttpServletResponse response) {
        response.addHeader("Set-Cookie", build("", Duration.ZERO).toString());
    }

    /** 요청에 실려 온 리프레시 토큰. 없으면 비어 있다. */
    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                // 자바스크립트가 못 읽는다 — XSS 로도 값을 빼갈 수 없다.
                .httpOnly(true)
                .secure(auth.cookieSecure())
                .sameSite(auth.cookieSameSite())
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
