package com.dabhaejwo.global.security;

import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.repository.BotRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.tenant.service.OriginCallRecorder;
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
 * 공개 키로 <b>서비스</b>를 찾고, 그 서비스에 등록된 Origin 인지 여기서 확인한다.
 *
 * <p><b>Origin 검증을 서비스 범위로 하는 것이 이 필터의 핵심이다.</b> 업체 범위로 두면
 * 한 업체의 모든 주소가 모든 키에 통용되어, A 서비스 키를 B 서비스 도메인에 붙여도 통과한다.
 *
 * <p>통과 조건이 <b>둘</b>이다 — 서비스가 켜져 있고(업체가 내리지 않았고), 업체 계약이
 * 살아 있어야 한다. 두 축을 나눈 이유는 정지됐던 업체를 되살릴 때 업체가 원래 꺼둔 서비스까지
 * 같이 켜지면 안 되기 때문이다.
 */
public class WidgetKeyAuthFilter extends OncePerRequestFilter {

    public static final String KEY_HEADER = "X-Dabhaejwo-Key";

    private final BotRepository botRepository;
    private final TenantRepository tenantRepository;
    private final AllowedOriginRepository allowedOriginRepository;
    private final OriginCallRecorder callRecorder;
    private final ObjectMapper objectMapper;

    public WidgetKeyAuthFilter(BotRepository botRepository,
                               TenantRepository tenantRepository,
                               AllowedOriginRepository allowedOriginRepository,
                               OriginCallRecorder callRecorder,
                               ObjectMapper objectMapper) {
        this.botRepository = botRepository;
        this.tenantRepository = tenantRepository;
        this.allowedOriginRepository = allowedOriginRepository;
        this.callRecorder = callRecorder;
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

        Optional<Bot> foundBot = botRepository.findByPublishableKey(key);
        if (foundBot.isEmpty() || !foundBot.get().servesVisitors()) {
            // 존재하지 않는 키와 내려둔 서비스를 구분해 주지 않는다 — 키 열거를 돕지 않기 위해서다.
            writeError(response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }

        Bot bot = foundBot.get();
        Optional<Tenant> foundTenant = tenantRepository.findById(bot.getTenantId());
        if (foundTenant.isEmpty() || !foundTenant.get().getStatus().servesVisitors()) {
            // 계약이 끊긴 업체. 여기도 같은 오류로 뭉갠다.
            writeError(response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }

        String origin = normalize(request.getHeader("Origin"));
        // **서비스 범위로 본다.** 업체 범위면 A 키를 B 도메인에 붙여도 통과한다.
        boolean allowed = allowedOriginRepository.findAllByBotId(bot.getId()).stream()
                .anyMatch(o -> normalize(o.getOrigin()).equalsIgnoreCase(origin));
        if (!allowed) {
            writeError(response, ErrorCode.ORIGIN_NOT_ALLOWED);
            return;
        }

        // 여기까지 왔다는 건 키와 주소가 모두 맞았다는 뜻이다 — 설치가 됐다는 신호다.
        // 이걸 안 남기면 업체는 붙였는지 아닌지를 설치 화면에서 확인할 수 없다.
        callRecorder.record(bot.getId(), origin);

        SecurityContextHolder.getContext().setAuthentication(
                PrincipalAuthentication.of(new AuthPrincipal.Visitor(bot.scope(), origin)));
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
