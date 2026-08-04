package com.dabhaejwo.domain.member.dto.request;

import com.dabhaejwo.global.security.TenantMemberRole;
import jakarta.validation.constraints.NotNull;

public record MemberRoleRequest(@NotNull TenantMemberRole role) {
}
