package com.dabhaejwo.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 임시 비밀번호로 본인을 확인하고 새 비밀번호를 만든다.
 *
 * @param temporaryPassword 메일로 받은 값. 한 번 쓰면 폐기된다
 */
public record ResetPasswordRequest(
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank String temporaryPassword,
        @NotBlank @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다") String newPassword) {
}
