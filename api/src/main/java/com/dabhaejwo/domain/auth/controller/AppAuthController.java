package com.dabhaejwo.domain.auth.controller;

import com.dabhaejwo.domain.auth.dto.request.AppLoginRequest;
import com.dabhaejwo.domain.auth.dto.request.RefreshRequest;
import com.dabhaejwo.domain.auth.dto.response.AppLoginResponse;
import com.dabhaejwo.domain.auth.dto.response.TokenResponse;
import com.dabhaejwo.domain.auth.dto.request.SignupRequest;
import com.dabhaejwo.domain.auth.service.AppAuthService;
import com.dabhaejwo.domain.auth.service.SignupService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.SignupRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 엔드포인트. {@code /api/auth/**} 는 인증 없이 접근한다. */
@RestController
@RequestMapping("/api/auth")
public class AppAuthController {

    private final AppAuthService appAuthService;
    private final SignupService signupService;
    private final SignupRateLimiter rateLimiter;

    public AppAuthController(AppAuthService appAuthService,
                             SignupService signupService,
                             SignupRateLimiter rateLimiter) {
        this.appAuthService = appAuthService;
        this.signupService = signupService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/app/login")
    public AppLoginResponse login(@Valid @RequestBody AppLoginRequest request) {
        return appAuthService.login(request);
    }

    /**
     * 가입. 성공하면 로그인과 같은 형태로 토큰을 돌려준다 —
     * 가입 직후 로그인 상태여야 하고, 클라이언트가 두 응답을 다르게 다룰 이유가 없다.
     */
    @PostMapping("/app/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AppLoginResponse signup(@Valid @RequestBody SignupRequest request,
                                   HttpServletRequest http) {
        if (!rateLimiter.tryAcquire(clientIp(http))) {
            throw new BusinessException(ErrorCode.RATE_LIMITED,
                    "가입 시도가 너무 잦습니다. 잠시 후 다시 시도해 주세요");
        }
        return signupService.signup(request);
    }

    /**
     * 클라이언트 IP. 프록시 뒤에 있으면 {@code X-Forwarded-For} 의 첫 값이 원 클라이언트다.
     *
     * <p>이 헤더는 <b>클라이언트가 위조할 수 있다.</b> 그래서 레이트 리밋 키로만 쓰고
     * 인가 판단에는 쓰지 않는다. 신뢰할 수 있으려면 프록시가 헤더를 덮어써야 한다.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return appAuthService.refresh(request.refreshToken());
    }
}
