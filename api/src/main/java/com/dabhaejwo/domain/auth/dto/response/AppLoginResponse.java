package com.dabhaejwo.domain.auth.dto.response;

import com.dabhaejwo.domain.member.dto.response.MemberResponse;

public record AppLoginResponse(
        String accessToken,
        String refreshToken,
        MemberResponse member) {
}
