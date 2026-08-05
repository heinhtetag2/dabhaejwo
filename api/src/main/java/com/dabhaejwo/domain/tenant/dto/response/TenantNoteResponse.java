package com.dabhaejwo.domain.tenant.dto.response;

import com.dabhaejwo.domain.tenant.entity.TenantNote;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 내부 메모. api-contracts.md §4. 수정·삭제 응답이 없는 이유는 그런 행위가 없기 때문이다. */
public record TenantNoteResponse(
        Long id,
        String body,
        OperatorRef operator,
        OffsetDateTime createdAt) {

    public record OperatorRef(UUID id, String name) {
    }

    public static TenantNoteResponse of(TenantNote note, OperatorRef operator) {
        return new TenantNoteResponse(note.getId(), note.getBody(), operator, note.getCreatedAt());
    }
}
