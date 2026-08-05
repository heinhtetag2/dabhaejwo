package com.dabhaejwo.domain.auth.controller;

import com.dabhaejwo.domain.auth.dto.request.AppLoginRequest;
import com.dabhaejwo.domain.auth.dto.request.ForgotPasswordRequest;
import com.dabhaejwo.domain.auth.dto.request.InviteAcceptRequest;
import com.dabhaejwo.domain.auth.dto.request.OtpVerifyRequest;
import com.dabhaejwo.domain.auth.dto.request.ResetPasswordRequest;
import com.dabhaejwo.domain.auth.dto.response.InvitePreviewResponse;
import com.dabhaejwo.domain.auth.dto.response.OtpChallengeResponse;
import com.dabhaejwo.domain.auth.entity.AuthScope;
import com.dabhaejwo.domain.auth.service.InviteService;
import com.dabhaejwo.domain.auth.service.PasswordResetService;
import com.dabhaejwo.domain.auth.dto.response.AppLoginResponse;
import com.dabhaejwo.domain.auth.dto.request.SignupRequest;
import com.dabhaejwo.domain.auth.service.AppAuthService;
import com.dabhaejwo.domain.auth.service.SignupService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.ClientIp;
import com.dabhaejwo.global.security.RefreshTokenCookie;
import com.dabhaejwo.global.security.SignupRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final PasswordResetService passwordResetService;
    private final InviteService inviteService;
    private final RefreshTokenCookie refreshCookie;

    public AppAuthController(AppAuthService appAuthService,
                             SignupService signupService,
                             SignupRateLimiter rateLimiter,
                             PasswordResetService passwordResetService,
                             InviteService inviteService,
                             RefreshTokenCookie refreshCookie) {
        this.appAuthService = appAuthService;
        this.signupService = signupService;
        this.rateLimiter = rateLimiter;
        this.passwordResetService = passwordResetService;
        this.inviteService = inviteService;
        this.refreshCookie = refreshCookie;
    }

    /**
     * 로그인 1단계. 비밀번호가 맞으면 <b>토큰이 아니라 챌린지</b>를 주고 코드를 메일로 보낸다.
     *
     * <p>임시 비밀번호로 들어오면 {@code PASSWORD_CHANGE_REQUIRED}(403) 다 —
     * 화면은 비밀번호 재설정으로 보낸다.
     */
    @PostMapping("/app/login")
    public OtpChallengeResponse login(@Valid @RequestBody AppLoginRequest request,
                                      HttpServletRequest http) {
        return appAuthService.login(request, ClientIp.hashOf(http));
    }

    /**
     * 로그인 2단계. 코드가 맞아야 토큰이 나온다.
     *
     * <p>리프레시 토큰은 본문과 <b>httpOnly 쿠키 양쪽</b>으로 나간다. 본문은 지금 이 탭이
     * 쓰고, 쿠키는 새로고침 뒤 세션을 되살리는 데 쓴다 — 자바스크립트가 못 읽는 곳에 둬야
     * XSS 로 털리지 않는다 ({@link RefreshTokenCookie}).
     */
    @PostMapping("/app/login/otp")
    public AppLoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request,
                                      HttpServletResponse response) {
        AppLoginResponse login = appAuthService.verifyOtp(request);
        refreshCookie.issue(response, AuthScope.APP, login.refreshToken());
        return login;
    }

    /**
     * 비밀번호 찾기 — 임시 비밀번호 발송.
     *
     * <p>계정이 없어도 <b>204 다.</b> 응답이 갈리면 어떤 주소가 가입돼 있는지 확인하는 도구가 된다.
     */
    @PostMapping("/app/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgot(AuthScope.APP, request);
    }

    /** 임시 비밀번호로 본인을 확인하고 새 비밀번호를 만든다. */
    @PostMapping("/app/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(AuthScope.APP, request);
    }

    /** 초대 링크를 열었을 때. 비밀번호를 정하기 전에 어디에 초대됐는지 보여준다. */
    @GetMapping("/app/invite")
    public InvitePreviewResponse invitePreview(@RequestParam String token) {
        return inviteService.preview(token);
    }

    /** 초대 수락 — 비밀번호를 정한다. 끝나면 로그인 화면으로 보낸다. */
    @PostMapping("/app/invite/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptInvite(@Valid @RequestBody InviteAcceptRequest request) {
        inviteService.accept(request);
    }

    /**
     * 가입. 성공하면 로그인과 같은 형태로 토큰을 돌려준다 —
     * 가입 직후 로그인 상태여야 하고, 클라이언트가 두 응답을 다르게 다룰 이유가 없다.
     */
    @PostMapping("/app/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AppLoginResponse signup(@Valid @RequestBody SignupRequest request,
                                   HttpServletRequest http,
                                   HttpServletResponse response) {
        if (!rateLimiter.tryAcquire(ClientIp.of(http))) {
            throw new BusinessException(ErrorCode.RATE_LIMITED,
                    "가입 시도가 너무 잦습니다. 잠시 후 다시 시도해 주세요");
        }
        AppLoginResponse login = signupService.signup(request);
        refreshCookie.issue(response, AuthScope.APP, login.refreshToken());
        return login;
    }

}
