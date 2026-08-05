package com.dabhaejwo.domain.operator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정.
 *
 * <p>기존 비밀번호를 확인하지 않는다 — 잊어버린 사람을 위한 관리자 조작이기 때문이다.
 * 그래서 <b>사유가 필수고 감사 기록에 남는다.</b> 남의 계정 비밀번호를 조용히 바꿀 수 있으면
 * 그 계정으로 한 일이 누구 것인지 알 수 없게 된다.
 */
public record OperatorPasswordRequest(
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String reason) {
}
