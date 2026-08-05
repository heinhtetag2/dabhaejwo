package com.dabhaejwo.domain.auth.controller;

import com.dabhaejwo.domain.auth.dto.request.ForgotPasswordRequest;
import com.dabhaejwo.domain.auth.dto.request.OpsLoginRequest;
import com.dabhaejwo.domain.auth.dto.request.OtpVerifyRequest;
import com.dabhaejwo.domain.auth.dto.request.ResetPasswordRequest;
import com.dabhaejwo.domain.auth.dto.response.OtpChallengeResponse;
import com.dabhaejwo.domain.auth.entity.AuthScope;
import com.dabhaejwo.domain.auth.service.PasswordResetService;
import com.dabhaejwo.global.security.ClientIp;
import com.dabhaejwo.global.security.RefreshTokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.dabhaejwo.domain.auth.dto.request.RefreshRequest;
import com.dabhaejwo.domain.auth.dto.response.OpsLoginResponse;
import com.dabhaejwo.domain.auth.dto.response.TokenResponse;
import com.dabhaejwo.domain.auth.service.OpsAuthService;
import com.dabhaejwo.domain.auth.service.TokenRefreshService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 인증과 토큰 재발급. {@code /api/auth/**} 는 인증 없이 접근한다.
 *
 * <p>재발급이 여기 있는 이유는 경로가 주체별로 나뉘지 않기 때문이다 —
 * 하나의 {@code /api/auth/refresh} 가 토큰의 scope 를 보고 갈라진다.
 */
@RestController
@RequestMapping("/api/auth")
public class OpsAuthController {

    private final OpsAuthService opsAuthService;
    private final TokenRefreshService tokenRefreshService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookie refreshCookie;

    public OpsAuthController(OpsAuthService opsAuthService,
                             TokenRefreshService tokenRefreshService,
                             PasswordResetService passwordResetService,
                             RefreshTokenCookie refreshCookie) {
        this.opsAuthService = opsAuthService;
        this.tokenRefreshService = tokenRefreshService;
        this.passwordResetService = passwordResetService;
        this.refreshCookie = refreshCookie;
    }

    /** 로그인 1단계. 운영 콘솔은 전 업체 데이터를 보므로 2단계 인증을 건너뛰지 않는다. */
    @PostMapping("/ops/login")
    public OtpChallengeResponse login(@Valid @RequestBody OpsLoginRequest request,
                                      HttpServletRequest http) {
        return opsAuthService.login(request, ClientIp.hashOf(http));
    }

    /** 로그인 2단계. 리프레시 토큰은 본문과 httpOnly 쿠키 양쪽으로 나간다. */
    @PostMapping("/ops/login/otp")
    public OpsLoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request,
                                      HttpServletResponse response) {
        OpsLoginResponse login = opsAuthService.verifyOtp(request);
        refreshCookie.issue(response, login.refreshToken());
        return login;
    }

    @PostMapping("/ops/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgot(AuthScope.OPS, request);
    }

    @PostMapping("/ops/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(AuthScope.OPS, request);
    }

    /**
     * 액세스 토큰 재발급.
     *
     * <p><b>쿠키를 먼저 본다.</b> 새로고침 직후에는 메모리가 비어 있어 클라이언트가 보낼
     * 토큰이 없다 — 그 상황을 되살리는 것이 이 경로의 존재 이유다. 본문은 쿠키가 없는
     * 클라이언트(서버 간 호출·테스트)를 위해 남겨 둔다.
     *
     * <p>새 쿠키를 다시 내려 <b>수명을 갱신한다.</b> 그러지 않으면 매일 쓰는 사용자도
     * 14일째에 갑자기 로그아웃된다.
     */
    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody(required = false) RefreshRequest request,
                                 HttpServletRequest http,
                                 HttpServletResponse response) {
        String token = refreshCookie.read(http)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> request == null ? null : request.refreshToken());
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        TokenResponse refreshed = tokenRefreshService.refresh(token);
        refreshCookie.issue(response, token);
        return refreshed;
    }

    /**
     * 로그아웃 — 쿠키를 지운다.
     *
     * <p>화면에서 메모리만 비우면 새로고침 한 번에 <b>다시 로그인된 상태로 돌아온다</b>.
     * 공용 PC 에서는 사고이므로 서버가 쿠키를 지워 줘야 한다.
     *
     * <p>인증을 요구하지 않는다. 이미 만료된 토큰을 들고 로그아웃하는 것이 정상적인 흐름이고,
     * 남의 쿠키를 지울 수는 없으므로(브라우저가 자기 것만 보낸다) 열어도 위험이 없다.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response) {
        refreshCookie.clear(response);
    }
}
