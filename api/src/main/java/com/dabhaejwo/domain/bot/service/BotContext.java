package com.dabhaejwo.domain.bot.service;

import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.repository.BotRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.BotScope;
import com.dabhaejwo.global.security.BotScopeInterceptor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 지금 대시보드가 다루고 있는 서비스.
 *
 * <p>범위는 <b>경로</b>에서 온다({@code /api/app/bots/{botId}/…}). 소유 업체 대조는
 * {@link com.dabhaejwo.global.security.BotScopeInterceptor} 가 이미 끝냈다.
 *
 * <p><b>경로에 서비스가 없으면 기본 서비스로 떨어지지 않고 거절한다.</b> 조용히 떨어뜨리면
 * 화면이 엉뚱한 서비스의 데이터를 자기 것으로 보여주고, 아무 오류도 나지 않는다.
 * 실패는 시끄러워야 한다.
 *
 * <p>화면 용어는 "서비스", 코드 용어는 {@code bot} 이다 — {@code docs/plan/service-plan.md} §3.
 */
@Service
public class BotContext {

    private final BotRepository botRepository;

    public BotContext(BotRepository botRepository) {
        this.botRepository = botRepository;
    }

    /** 지금 요청이 다루는 서비스. 경로에 없으면 거절한다. */
    public BotScope scope() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Object scope = attributes == null ? null
                : attributes.getRequest().getAttribute(BotScopeInterceptor.ATTRIBUTE);
        if (scope instanceof BotScope found) {
            return found;
        }
        // 경로에 서비스가 없는 곳에서 서비스 데이터를 만지려 한 것이다. 배선 실수다.
        throw new BusinessException(ErrorCode.BOT_NOT_FOUND);
    }

    /** 업체의 기본 서비스. 없으면 가입이 반쪽으로 끝난 것이므로 조용히 넘기지 않는다. */
    @Transactional(readOnly = true)
    public Bot defaultBot(UUID tenantId) {
        return botRepository.findFirstByTenantIdAndIsDefaultTrue(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOT_NOT_FOUND));
    }
}
