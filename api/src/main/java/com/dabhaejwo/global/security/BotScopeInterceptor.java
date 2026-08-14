package com.dabhaejwo.global.security;

import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.repository.BotRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

/**
 * 경로에 실린 서비스를 꺼내 <b>소유 업체를 대조</b>한다.
 *
 * <p>이 대조가 이 클래스의 전부다. 없으면 <b>남의 업체 botId 를 URL 에 넣는 것만으로
 * 대화 로그를 읽을 수 있다.</b>
 *
 * <p>실패는 {@code 404} 다. {@code 403} 으로 주면 "그 id 는 존재한다"를 알려주는 셈이라,
 * id 를 훑어 남의 서비스 존재 여부를 알아낼 수 있다 — 위젯 키 열거를 막는 것과 같은 이유다.
 *
 * <p>범위를 <b>경로</b>에 둔 이유: 헤더나 쿼리는 빼먹을 수 있고, 빼먹으면 기본 서비스로
 * 조용히 떨어진다. 이 프로젝트가 반복해서 당한 사고가 정확히 그 종류다
 * ({@code pageScope} 가 읽히지 않던 것, {@code markCalled} 가 호출되지 않던 것).
 * 경로면 빠질 수가 없고, 접근 로그만으로 어느 서비스 요청인지 안다.
 */
@Component
public class BotScopeInterceptor implements HandlerInterceptor {

    public static final String ATTRIBUTE = BotScopeInterceptor.class.getName() + ".scope";

    private final BotRepository botRepository;

    public BotScopeInterceptor(BotRepository botRepository) {
        this.botRepository = botRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        Object raw = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(raw instanceof Map<?, ?> vars)) {
            return true;
        }
        Object botId = vars.get("botId");
        if (botId == null) {
            return true;
        }

        UUID id;
        try {
            id = UUID.fromString(botId.toString());
        } catch (IllegalArgumentException e) {
            // 형식이 틀린 것도 "없다"로 답한다. 형식을 알려주면 훑는 데 도움이 된다.
            throw new BusinessException(ErrorCode.BOT_NOT_FOUND);
        }

        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        Bot bot = botRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOT_NOT_FOUND));

        request.setAttribute(ATTRIBUTE, bot.scope());
        return true;
    }
}
