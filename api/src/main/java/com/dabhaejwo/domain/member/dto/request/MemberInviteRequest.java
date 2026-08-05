package com.dabhaejwo.domain.member.dto.request;

import com.dabhaejwo.global.security.TenantMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 팀원 초대.
 *
 * @param phone 계정이 막혔을 때 연락할 수단. 메일 하나뿐이면 메일을 못 받는 순간 길이 없다.
 *              필수는 아니다 — 받겠다고 막으면 초대가 늦어진다
 */
public record MemberInviteRequest(
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 40) String name,
        @NotNull TenantMemberRole role,
        @Size(max = 30) @Pattern(regexp = "^$|^[0-9+\\-() ]{7,30}$",
                message = "전화번호 형식이 올바르지 않습니다") String phone) {
}
