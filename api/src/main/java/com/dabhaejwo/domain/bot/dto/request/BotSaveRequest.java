package com.dabhaejwo.domain.bot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 서비스 추가·이름 변경.
 *
 * <p>입력이 둘뿐인 것이 의도다 — 말투·모양은 기본값으로 만들고 나중에 고치게 한다.
 * 만들 때 다 물으면 "코드 붙이기"라는 다음 행동이 뒤로 밀린다.
 */
public record BotSaveRequest(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Size(max = 253) String primaryDomain) {
}
