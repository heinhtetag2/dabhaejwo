package com.dabhaejwo.domain.tenant.dto.response;

import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.entity.BotStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 운영 콘솔이 보는 서비스 한 줄.
 *
 * <p>업체 대시보드의 {@code BotResponse} 와 <b>겹치는 필드는 이름·타입이 완전히 같다</b>
 * (api-contract-rules 단일 표현). 운영자에게만 필요한 것(허용 주소)만 더한다.
 *
 * <p><b>여기가 운영 콘솔에서 허용 도메인을 처음 보게 되는 지점이다.</b> CS 문의 1순위가
 * "코드 붙였는데 안 떠요"이고 원인 대부분이 미등록 주소인데, 지금까지는 그걸 보려고
 * 사유를 적고 대리 로그인을 해야 했다.
 *
 * @param lastCalledAt null 이면 위젯이 아직 한 번도 호출되지 않았다 — 설치가 안 됐다는 뜻이다
 */
public record OpsBotResponse(
        UUID id,
        String name,
        String primaryDomain,
        String publishableKey,
        BotStatus status,
        boolean defaultBot,
        OffsetDateTime lastCalledAt,
        OffsetDateTime createdAt,
        List<AllowedOriginResponse> allowedOrigins) {

    public static OpsBotResponse of(Bot bot, List<AllowedOriginResponse> origins) {
        OffsetDateTime lastCalled = origins.stream()
                .map(AllowedOriginResponse::lastCalledAt)
                .filter(java.util.Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
        return new OpsBotResponse(bot.getId(), bot.getName(), bot.getPrimaryDomain(),
                bot.getPublishableKey(), bot.getStatus(), bot.isDefault(),
                lastCalled, bot.getCreatedAt(), origins);
    }
}
