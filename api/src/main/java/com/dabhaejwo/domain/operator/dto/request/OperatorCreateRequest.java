package com.dabhaejwo.domain.operator.dto.request;

import com.dabhaejwo.global.security.OperatorRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 운영자 등록.
 *
 * <p>초기 비밀번호를 관리자가 직접 정한다 — 초대 메일을 보낼 {@code Mailer} 가 아직
 * 로그 stub 이기 때문이다. 메일이 붙으면 초대 링크 방식으로 바꾼다
 * (CLAUDE.md Stub 목록).
 *
 * @param reason 사유 필수. 권한을 새로 만드는 행위다
 */
public record OperatorCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 50) String name,
        @NotNull OperatorRole role,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String reason) {
}
