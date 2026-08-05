package com.dabhaejwo.domain.auth.dto.response;

import com.dabhaejwo.domain.operator.dto.response.OperatorResponse;

/** 업체 담당자 로그인({@link AppLoginResponse})과 같은 형태다. 주체만 다르다. */
public record OpsLoginResponse(
        String accessToken,
        String refreshToken,
        OperatorResponse operator) {
}
