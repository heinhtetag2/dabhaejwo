package com.dabhaejwo.domain.provider.dto.response;

import com.dabhaejwo.global.llm.LlmProviderName;

import java.time.OffsetDateTime;

/**
 * 공급사 연결 상태. api-contracts.md §7.
 *
 * <p><b>API 키 원문을 담는 필드가 없다.</b> 있으면 언젠가 채워진다 — 마스킹된 힌트만 준다.
 *
 * @param source 키가 어디서 왔는가. {@code CONSOLE}(콘솔 등록) · {@code ENV}(환경변수 대체) ·
 *               {@code NONE}(없음). 운영자가 "왜 아직 예전 키로 도나"를 알 수 있어야 한다
 */
public record ProviderCredentialResponse(
        LlmProviderName provider,
        boolean configured,
        boolean enabled,
        String keyHint,
        Source source,
        OffsetDateTime updatedAt,
        String updatedByName) {

    public enum Source {
        CONSOLE,
        ENV,
        NONE
    }
}
