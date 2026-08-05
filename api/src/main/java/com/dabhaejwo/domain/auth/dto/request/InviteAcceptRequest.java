package com.dabhaejwo.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 초대 수락. 링크의 토큰으로 본인을 확인하고 비밀번호를 정한다.
 *
 * <p>확인용 재입력은 <b>화면에서</b> 맞춘다 — 서버에 두 번 보내면 두 값이 다를 때
 * 무엇이 맞는지 서버가 정할 근거가 없다.
 */
public record InviteAcceptRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다") String password) {
}
