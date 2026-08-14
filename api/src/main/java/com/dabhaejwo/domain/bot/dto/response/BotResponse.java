package com.dabhaejwo.domain.bot.dto.response;

import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.entity.BotStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 서비스 한 벌.
 *
 * <p>화면 용어는 "서비스", JSON 키는 {@code bot} 계열이다 —
 * {@code docs/plan/service-plan.md} §3.
 *
 * @param publishableKey 설치 스니펫에 들어가는 값. <b>서비스마다 다르다</b>
 * @param lastCalledAt   위젯이 실제로 호출한 시각. null 이면 아직 설치가 확인되지 않았다 —
 *                       화면은 이걸로 "작동 중"을 판정한다
 */
public record BotResponse(
        UUID id,
        String name,
        String primaryDomain,
        String publishableKey,
        BotStatus status,
        boolean defaultBot,
        OffsetDateTime lastCalledAt,
        OffsetDateTime createdAt) {

    public static BotResponse from(Bot bot, OffsetDateTime lastCalledAt) {
        return new BotResponse(bot.getId(), bot.getName(), bot.getPrimaryDomain(),
                bot.getPublishableKey(), bot.getStatus(), bot.isDefault(),
                lastCalledAt, bot.getCreatedAt());
    }
}
