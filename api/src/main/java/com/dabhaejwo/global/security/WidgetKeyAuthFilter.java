package com.dabhaejwo.global.security;

import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 위젯 인증: 공개 키 + Origin 검증.
 *
 * <p>신뢰 경계는 서버다. 위젯이 보내는 어떤 값도 그 자체로는 신뢰하지 않는다 —
 * 공개 키로 테넌트를 찾고, 그 테넌트에 등록된 Origin 인지 여기서 확인한다.
 */
public class WidgetKeyAuthFilter extends OncePerRequestFilter {

    public static final String KEY_HEADER = "X-Dabhaejwo-Key";

    private final TenantRepository tenantRepository;
    private final AllowedOriginRepository allowedOriginRepository;
    private final ObjectMapper objectMapper;

    public WidgetKeyAuthFilter(TenantRepository tenantRepository,
                               AllowedOriginRepository allowedOriginRepository,
                               ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.allowedOriginRepository = allowedOriginRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(KEY_HEADER);
        if (key == null || key.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Tenant> found = tenantRepository.findByPublishableKey(key);
        if (found.isEmpty() || !found.get().getStatus().servesVisitors()) {
            // 존재하지 않는 키와 정지된 업체를 구분해 주지 않는다 — 키 열거를 돕지 않기 위해서다.
            writeError(response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }

        Tenant tenant = found.get();
        String origin = normalize(request.getHeader("Origin"));
        boolean allowed = allowedOriginRepository.findAllByTenantId(tenant.getId()).stream()
                .anyMatch(o -> normalize(o.getOrigin()).equalsIgnoreCase(origin));
        if (!allowed) {
            writeError(response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                PrincipalAuthentication.of(new AuthPrincipal.Visitor(tenant.getId(), origin)));
        chain.doFilter(request, response);
    }

    /** {@code https://shop.example.com:443/} 과 {@code shop.example.com} 을 같게 본다. */
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        try {
            URI uri = URI.create(trimmed.contains("://") ? trimmed : "https://" + trimmed);
            return uri.getHost() == null ? trimmed : uri.getHost();
        } catch (IllegalArgumentException e) {
            return trimmed;
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
