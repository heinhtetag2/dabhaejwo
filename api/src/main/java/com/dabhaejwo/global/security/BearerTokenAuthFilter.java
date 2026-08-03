package com.dabhaejwo.global.security;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
// Spring Boot 4 는 Jackson 3 을 쓴다 — 패키지가 com.fasterxml.jackson 이 아니라 tools.jackson 이다.
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * {@code Authorization: Bearer} 토큰을 읽어 SecurityContext 를 채운다.
 * 운영자 체인과 업체 체인이 같은 로직을 쓰되 파싱 함수만 다르다 —
 * 토큰 종류 검증은 {@link JwtProvider} 의 {@code typ} 클레임이 한다.
 */
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final Function<String, ? extends AuthPrincipal> parser;
    private final ObjectMapper objectMapper;

    public BearerTokenAuthFilter(Function<String, ? extends AuthPrincipal> parser,
                                 ObjectMapper objectMapper) {
        this.parser = parser;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            // 토큰이 없으면 익명으로 통과시키고, 인가는 뒤의 authorizeHttpRequests 가 막는다.
            chain.doFilter(request, response);
            return;
        }
        try {
            AuthPrincipal principal = parser.apply(header.substring(BEARER.length()));
            SecurityContextHolder.getContext().setAuthentication(PrincipalAuthentication.of(principal));
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            writeError(response, e.errorCode());
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
