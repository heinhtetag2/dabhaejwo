package com.dabhaejwo.global.config;

import com.dabhaejwo.global.security.BotScopeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 경로에 실린 서비스({@code botId})를 요청 시작 시 한 번 해석하고 소유 업체를 대조한다.
 *
 * <p>서비스 데이터를 다루는 모든 경로가 {@code /api/app/bots/{botId}/…} 아래에 있으므로
 * 한 곳에서 건다. 컨트롤러마다 검증을 흩으면 언젠가 한 곳이 빠지고,
 * 빠진 그곳이 <b>남의 업체 데이터를 여는 문</b>이 된다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final BotScopeInterceptor botScopeInterceptor;

    public WebMvcConfig(BotScopeInterceptor botScopeInterceptor) {
        this.botScopeInterceptor = botScopeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(botScopeInterceptor).addPathPatterns("/api/app/bots/**");
    }
}
