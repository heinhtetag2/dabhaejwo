package com.dabhaejwo.global.security;

import java.util.UUID;

/**
 * 지금 다루는 서비스의 범위.
 *
 * <p><b>{@code tenantId} 와 {@code botId} 를 절대 따로 넘기지 않기 위해 존재한다.</b>
 * 같은 타입 UUID 두 개가 메서드 인자로 나란히 다니면 언젠가 순서가 바뀌고
 * <b>컴파일러가 못 잡는다.</b> 그 결과는 타 업체 데이터 조회이므로 P0 다.
 *
 * <p>리포지토리 층에서만 풀어 쓴다. 서비스 레이어는 이 값을 통째로 주고받는다.
 */
public record BotScope(UUID tenantId, UUID botId) {

    public BotScope {
        if (tenantId == null || botId == null) {
            throw new IllegalArgumentException("BotScope 는 두 값을 모두 요구한다");
        }
    }
}
