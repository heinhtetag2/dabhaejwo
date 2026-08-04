package com.dabhaejwo.domain.auth.controller;

import com.dabhaejwo.domain.auth.dto.request.AppLoginRequest;
import com.dabhaejwo.domain.auth.dto.request.RefreshRequest;
import com.dabhaejwo.domain.auth.dto.response.AppLoginResponse;
import com.dabhaejwo.domain.auth.dto.response.TokenResponse;
import com.dabhaejwo.domain.auth.service.AppAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 공개 엔드포인트. {@code /api/auth/**} 는 인증 없이 접근한다. */
@RestController
@RequestMapping("/api/auth")
public class AppAuthController {

    private final AppAuthService appAuthService;

    public AppAuthController(AppAuthService appAuthService) {
        this.appAuthService = appAuthService;
    }

    @PostMapping("/app/login")
    public AppLoginResponse login(@Valid @RequestBody AppLoginRequest request) {
        return appAuthService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return appAuthService.refresh(request.refreshToken());
    }
}
