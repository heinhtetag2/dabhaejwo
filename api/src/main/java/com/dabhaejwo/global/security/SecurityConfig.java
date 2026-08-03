package com.dabhaejwo.global.security;

import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증 체계 3종 분리.
 *
 * <p>운영자·업체 담당자·방문자는 신뢰 수준이 전혀 다르다. 하나의 체인에서 역할로 갈라내면
 * 언젠가 경계가 새기 때문에 URL prefix 부터 체인을 나눈다.
 *
 * <pre>
 * /api/ops/**    운영자 JWT
 * /api/app/**    업체 담당자 JWT (대리 로그인 토큰 포함)
 * /api/widget/** 공개 키 + Origin
 * /api/auth/**   공개
 * </pre>
 */
@Configuration
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;

    public SecurityConfig(ObjectMapper objectMapper, JwtProvider jwtProvider) {
        this.objectMapper = objectMapper;
        this.jwtProvider = jwtProvider;
    }

    @Bean
    @Order(1)
    SecurityFilterChain opsChain(HttpSecurity http) throws Exception {
        return baseline(http.securityMatcher("/api/ops/**"))
                .addFilterBefore(
                        new BearerTokenAuthFilter(jwtProvider::parseOperator, objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain appChain(HttpSecurity http) throws Exception {
        return baseline(http.securityMatcher("/api/app/**"))
                .addFilterBefore(
                        new BearerTokenAuthFilter(jwtProvider::parseTenantUser, objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain widgetChain(HttpSecurity http,
                                    TenantRepository tenantRepository,
                                    AllowedOriginRepository allowedOriginRepository) throws Exception {
        return baseline(http.securityMatcher("/api/widget/**"))
                .addFilterBefore(
                        new WidgetKeyAuthFilter(tenantRepository, allowedOriginRepository, objectMapper),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        return baseline(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    /** 세 체인 공통 — 상태 없는 토큰 인증이라 세션과 CSRF 토큰을 쓰지 않는다. */
    private HttpSecurity baseline(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
