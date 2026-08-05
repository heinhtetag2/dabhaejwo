package com.dabhaejwo.domain.operator.dto.response;

import com.dabhaejwo.domain.operator.entity.Operator;
import com.dabhaejwo.global.security.OperatorRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 운영자. api-contracts.md §1.
 *
 * <p>비밀번호 해시와 TOTP 시크릿은 어떤 응답에도 실리지 않는다.
 */
public record OperatorResponse(
        UUID id,
        String name,
        String email,
        OperatorRole role,
        boolean active,
        OffsetDateTime lastSeenAt) {

    public static OperatorResponse from(Operator operator) {
        return new OperatorResponse(
                operator.getId(),
                operator.getName(),
                operator.getEmail(),
                operator.getRole(),
                operator.isActive(),
                operator.getLastSeenAt());
    }
}
