package com.dabhaejwo.domain.audit.dto.response;

import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLog;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 감사 기록 한 줄. api-contracts.md §8.
 *
 * <p>쓰기 DTO 가 없는 것이 설계다 — 적재는 서버 내부에서만 일어나고,
 * 수정·삭제는 DB 트리거가 막는다.
 */
public record AuditLogResponse(
        Long id,
        OffsetDateTime at,
        OperatorRef operator,
        AuditAction action,
        TenantRef tenant,
        String reason,
        Map<String, Object> meta) {

    public record OperatorRef(UUID id, String name) {
    }

    public record TenantRef(UUID id, String name) {
    }

    public static AuditLogResponse of(AuditLog log, String operatorName, String tenantName) {
        return new AuditLogResponse(
                log.getId(),
                log.getCreatedAt(),
                new OperatorRef(log.getOperatorId(), operatorName),
                log.getAction(),
                // 업체와 무관한 행위(단가 수정·기능 공개)는 tenant 가 null 이다.
                log.getTenantId() == null ? null : new TenantRef(log.getTenantId(), tenantName),
                log.getReason(),
                log.getMeta());
    }
}
