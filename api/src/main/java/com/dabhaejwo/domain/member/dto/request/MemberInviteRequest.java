package com.dabhaejwo.domain.member.dto.request;

import com.dabhaejwo.global.security.TenantMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemberInviteRequest(
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 40) String name,
        @NotNull TenantMemberRole role) {
}
