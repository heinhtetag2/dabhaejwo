package com.dabhaejwo.domain.auth.service;

import com.dabhaejwo.domain.auth.dto.response.TokenResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.JwtProvider;
import org.springframework.stereotype.Service;

/**
 * {@code POST /api/auth/refresh} 는 하나뿐이다. 리프레시 토큰의 {@code scope} 를 읽어
 * <b>원래 주체 종류로만</b> 액세스 토큰을 다시 만든다.
 *
 * <p>엔드포인트를 주체별로 쪼개지 않은 이유는, 쪼개면 클라이언트가 자기 주체 종류를
 * 경로로 주장하게 되고 그 주장을 서버가 다시 검증해야 하기 때문이다. 토큰 안의 scope 가
 * 이미 진실이므로 경로로 되풀이하지 않는다.
 */
@Service
public class TokenRefreshService {

    private final JwtProvider jwtProvider;
    private final AppAuthService appAuthService;
    private final OpsAuthService opsAuthService;

    public TokenRefreshService(JwtProvider jwtProvider,
                               AppAuthService appAuthService,
                               OpsAuthService opsAuthService) {
        this.jwtProvider = jwtProvider;
        this.appAuthService = appAuthService;
        this.opsAuthService = opsAuthService;
    }

    public TokenResponse refresh(String refreshToken) {
        JwtProvider.RefreshSubject subject = jwtProvider.parseRefresh(refreshToken);
        return switch (subject.scope() == null ? "" : subject.scope()) {
            case AppAuthService.SCOPE_APP -> appAuthService.refresh(subject);
            case OpsAuthService.SCOPE_OPS -> opsAuthService.refresh(subject);
            // 알 수 없는 scope 는 어느 쪽으로도 보내지 않는다. 기본 분기를 두면
            // scope 가 빠진 토큰이 조용히 한쪽으로 흘러간다.
            default -> throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        };
    }
}
