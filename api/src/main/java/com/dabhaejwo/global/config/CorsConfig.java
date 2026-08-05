package com.dabhaejwo.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS.
 *
 * <p>콘솔·대시보드는 API 와 다른 출처에서 돈다(4311·4312 ↔ 4310). 브라우저가 프리플라이트를
 * 보내므로 서버가 허용 출처를 밝혀야 한다.
 *
 * <p><b>CORS 는 보안 경계가 아니다.</b> 브라우저가 응답을 읽게 할지를 정할 뿐이고,
 * curl 이나 서버 대 서버 호출에는 아무 영향이 없다. 실제 차단은 토큰과 필터 체인이 한다.
 * 그래서 위젯 경로는 모든 출처를 허용하고, 그 대신 {@code WidgetKeyAuthFilter} 가
 * 테넌트에 등록된 주소인지 확인해 403 을 낸다 — 프리플라이트에는 공개 키 값이 실리지 않아
 * 그 시점에는 어느 테넌트인지 알 수 없기 때문이다.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /** 프리플라이트 결과를 브라우저가 캐시하는 시간(초). 매 요청마다 왕복하지 않게 한다. */
    private static final long MAX_AGE_SECONDS = 3600;

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        List<String> allowedOrigins = properties.cors().allowedOrigins().stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::strip)
                .toList();

        if (allowedOrigins.isEmpty()) {
            // 조용히 넘어가면 "왜 브라우저에서만 안 되지"를 한참 헤맨다.
            log.warn("dabhaejwo.cors.allowed-origins 가 비어 있습니다. "
                    + "브라우저에서 오는 콘솔·대시보드 요청이 전부 차단됩니다.");
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/widget/**", widgetConfig());
        source.registerCorsConfiguration("/api/**", consoleConfig(allowedOrigins));
        return source;
    }

    /** 운영 콘솔·업체 대시보드. 설정에 적힌 출처만 허용한다. */
    private CorsConfiguration consoleConfig(List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT));
        // CSV 내보내기에서 파일명을 읽으려면 이 헤더가 노출돼야 한다.
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        /*
         * 리프레시 토큰이 httpOnly 쿠키로 오간다(RefreshTokenCookie). 쿠키를 실으려면
         * 브라우저 쪽 `credentials: "include"` 와 서버 쪽 이 플래그가 <b>둘 다</b> 켜져야 한다.
         *
         * 이 값이 true 면 `Access-Control-Allow-Origin: *` 를 쓸 수 없다 — 위에서 출처를
         * 명시 목록으로 받는 이유가 이것이다. 와일드카드로 바꾸는 순간 브라우저가 거부한다.
         */
        config.setAllowCredentials(true);
        config.setMaxAge(MAX_AGE_SECONDS);
        return config;
    }

    /** 위젯. 출처를 미리 알 수 없으므로 열어두고, 실제 판정은 필터가 한다. */
    private CorsConfiguration widgetConfig() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("X-Dabhaejwo-Key", HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT));
        config.setAllowCredentials(false);
        config.setMaxAge(MAX_AGE_SECONDS);
        return config;
    }
}
