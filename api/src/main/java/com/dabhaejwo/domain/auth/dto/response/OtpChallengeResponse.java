package com.dabhaejwo.domain.auth.dto.response;

import com.dabhaejwo.domain.auth.service.LoginChallengeService;

import java.util.UUID;

/**
 * 비밀번호는 맞았고, 이제 메일로 간 코드가 필요하다.
 *
 * <p><b>토큰이 없다.</b> 이 응답만으로는 아무것도 할 수 없다 — 다음 단계를 통과해야 토큰이 나온다.
 *
 * @param maskedEmail 어디로 보냈는지 알려준다. 주소를 그대로 돌려주면 계정 확인 수단이 되므로 가린다
 */
public record OtpChallengeResponse(UUID challengeId, String maskedEmail, int ttlMinutes) {

    public static OtpChallengeResponse of(LoginChallengeService.Issued issued, String email) {
        return new OtpChallengeResponse(issued.challengeId(), mask(email), issued.ttlMinutes());
    }

    /** {@code ab***@example.com}. 본인은 알아보고 남은 못 알아본다. */
    static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String keep = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return keep + "***" + email.substring(at);
    }
}
