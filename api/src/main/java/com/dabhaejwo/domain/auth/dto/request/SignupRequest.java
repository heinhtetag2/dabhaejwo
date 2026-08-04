package com.dabhaejwo.domain.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 가입. 한 화면에 끝내야 하므로 항목이 다섯 개다 (tenant-public-plan.md §4.3).
 *
 * @param primaryDomain 학습 대상이자 위젯 허용 도메인. 스킴·경로를 붙여 보내도 서버가 호스트만 뗀다
 * @param termsAgreed   동의하지 않으면 가입 자체가 성립하지 않는다. 서버에서도 막는다 —
 *                      클라이언트에서 버튼을 비활성화하는 것은 UX 이지 검증이 아니다
 */
public record SignupRequest(
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다") String password,
        @NotBlank @Size(max = 60) String tenantName,
        @NotBlank @Size(max = 255) String primaryDomain,
        @AssertTrue(message = "약관에 동의해야 가입할 수 있습니다") boolean termsAgreed) {
}
