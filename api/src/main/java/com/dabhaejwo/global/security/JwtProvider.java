package com.dabhaejwo.global.security;

import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 토큰 발급·검증. 주체 종류를 {@code typ} 클레임으로 구분한다 —
 * 운영자 토큰으로 업체 API 를 부르거나 그 반대가 되면 안 된다.
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    public static final String TYPE_OPS = "ops";
    public static final String TYPE_APP = "app";
    /**
     * 리프레시 토큰. 액세스 토큰과 {@code typ} 이 달라서 리프레시 토큰을 그대로
     * {@code Authorization} 에 넣어도 API 를 호출할 수 없다.
     */
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TENANT = "tenantId";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_IMPERSONATION = "impersonationSessionId";
    private static final String CLAIM_OPERATOR = "impersonatingOperatorId";
    private static final String CLAIM_SCOPE = "scope";

    private final SecretKey key;
    private final AppProperties.Auth auth;

    public JwtProvider(AppProperties properties) {
        this.auth = properties.auth();
        this.key = resolveKey(auth.jwtSecret());
    }

    private SecretKey resolveKey(String secret) {
        if (secret == null || secret.isBlank()) {
            // 운영에서 이 경로를 타면 재시작마다 모든 토큰이 무효가 된다. 조용히 넘어가지 않는다.
            log.warn("JWT_SECRET 이 비어 있어 임시 키를 생성했습니다. 개발 환경에서만 허용됩니다.");
            return Jwts.SIG.HS256.key().build();
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueOperatorToken(UUID operatorId, String name, OperatorRole role) {
        return build(operatorId.toString(), Duration.ofMinutes(auth.accessTtlMinutes()))
                .claim(CLAIM_TYPE, TYPE_OPS)
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_ROLE, role.name())
                .compact();
    }

    public String issueTenantToken(UUID memberId, UUID tenantId, TenantMemberRole role) {
        return build(memberId.toString(), Duration.ofMinutes(auth.accessTtlMinutes()))
                .claim(CLAIM_TYPE, TYPE_APP)
                .claim(CLAIM_TENANT, tenantId.toString())
                .claim(CLAIM_ROLE, role.name())
                .compact();
    }

    /**
     * 대리 로그인 토큰. 업체 담당자 토큰과 같은 체인을 타지만 세션 id 를 달고 다니므로
     * 파괴적 조작 차단과 감사 기록이 걸린다. 만료는 업체 토큰과 무관하게 30분이다.
     */
    public String issueImpersonationToken(UUID sessionId,
                                          UUID operatorId,
                                          UUID tenantId) {
        return build(sessionId.toString(), Duration.ofMinutes(auth.impersonationTtlMinutes()))
                .claim(CLAIM_TYPE, TYPE_APP)
                .claim(CLAIM_TENANT, tenantId.toString())
                // 대리 접속자는 업체 데이터를 볼 수 있지만 편집 권한은 VIEWER 로 제한한다.
                .claim(CLAIM_ROLE, TenantMemberRole.VIEWER.name())
                .claim(CLAIM_IMPERSONATION, sessionId.toString())
                .claim(CLAIM_OPERATOR, operatorId.toString())
                .compact();
    }

    /**
     * 리프레시 토큰 발급. 어느 체계의 주체인지({@code scope}) 함께 담아, 재발급 시
     * 원래 주체 종류로만 액세스 토큰을 만들 수 있게 한다.
     *
     * <p>서버에 저장하지 않는 상태 없는 토큰이라 개별 무효화가 불가능하다. 로그아웃은
     * 클라이언트가 토큰을 버리는 것으로 처리한다. 강제 무효화가 필요해지면 저장소를
     * 붙여야 한다 — docs/IMPROVEMENTS.md 참조.
     */
    public String issueRefreshToken(UUID subjectId, String scope) {
        return build(subjectId.toString(), Duration.ofDays(auth.refreshTtlDays()))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_SCOPE, scope)
                .compact();
    }

    /** @return 리프레시 토큰의 주체 id 와 scope. */
    public RefreshSubject parseRefresh(String token) {
        Claims claims = parse(token, TYPE_REFRESH);
        return new RefreshSubject(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_SCOPE, String.class));
    }

    public record RefreshSubject(UUID subjectId, String scope) {
    }

    private io.jsonwebtoken.JwtBuilder build(String subject, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key);
    }

    public AuthPrincipal.Operator parseOperator(String token) {
        Claims claims = parse(token, TYPE_OPS);
        return new AuthPrincipal.Operator(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_NAME, String.class),
                OperatorRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    public AuthPrincipal.TenantUser parseTenantUser(String token) {
        Claims claims = parse(token, TYPE_APP);
        String impersonationId = claims.get(CLAIM_IMPERSONATION, String.class);
        String operatorId = claims.get(CLAIM_OPERATOR, String.class);
        return new AuthPrincipal.TenantUser(
                UUID.fromString(claims.get(CLAIM_TENANT, String.class)),
                UUID.fromString(claims.getSubject()),
                TenantMemberRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
                impersonationId == null ? null : UUID.fromString(impersonationId),
                operatorId == null ? null : UUID.fromString(operatorId));
    }

    private Claims parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                // 운영자 토큰으로 업체 API 를 호출하는 등의 교차 사용을 막는다.
                throw new BusinessException(ErrorCode.UNAUTHENTICATED);
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
