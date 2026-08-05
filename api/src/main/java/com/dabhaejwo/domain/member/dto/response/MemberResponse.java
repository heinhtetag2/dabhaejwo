package com.dabhaejwo.domain.member.dto.response;

import com.dabhaejwo.domain.member.entity.InviteState;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.global.security.TenantMemberRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체 담당자. api-contracts.md §9-0.
 *
 * <p>비밀번호 해시는 어떤 응답에도 실리지 않는다.
 */
public record MemberResponse(
        UUID id,
        String name,
        String email,
        TenantMemberRole role,
        InviteState inviteState,
        String phone,
        OffsetDateTime lastSeenAt) {

    public static MemberResponse from(TenantMember member) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getRole(),
                member.getInviteState(),
                member.getPhone(),
                member.getLastSeenAt());
    }
}
