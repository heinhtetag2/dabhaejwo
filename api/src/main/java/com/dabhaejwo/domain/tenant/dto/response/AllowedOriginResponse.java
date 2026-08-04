package com.dabhaejwo.domain.tenant.dto.response;

import com.dabhaejwo.domain.tenant.entity.AllowedOrigin;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 위젯이 동작해도 되는 주소.
 *
 * <p>공개 키({@code pk_live_*})는 남의 사이트 소스에 그대로 노출된다. 그래도 안전한 이유가
 * 이 화이트리스트다 — 등록되지 않은 도메인에서는 서버가 거부한다.
 *
 * @param lastCalledAt 호출이 한 번도 없으면 null. 설치가 끝났는지 판단하는 근거다
 */
public record AllowedOriginResponse(
        UUID id,
        String origin,
        OffsetDateTime lastCalledAt) {

    public static AllowedOriginResponse from(AllowedOrigin origin) {
        return new AllowedOriginResponse(origin.getId(), origin.getOrigin(), origin.getLastCalledAt());
    }
}
