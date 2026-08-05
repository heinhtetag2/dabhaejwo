package com.dabhaejwo.domain.operator.dto.request;

import com.dabhaejwo.global.security.OperatorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 이름·역할 수정.
 *
 * <p>{@code email} 이 없는 것이 설계다 — 로그인 식별자이자 감사 기록이 가리키는 사람이다.
 * 바꿀 수 있게 두면 과거 기록의 행위자가 다른 사람처럼 보인다. 사람이 바뀌면 새 계정을 만든다.
 */
public record OperatorUpdateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull OperatorRole role,
        @NotBlank String reason) {
}
