package com.dabhaejwo.global.security;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 가입 시도 제한.
 *
 * <p>이메일 인증이 없어 가짜 주소로 체험 계정을 반복 생성할 수 있다
 * (tenant-public-plan.md §5.4). 체험 한도와 일일 원가 상한이 피해를 제한하지만,
 * 만들어지는 것 자체를 늦춰 두는 편이 낫다.
 *
 * <p>계산은 {@link SlidingWindowLimiter} 에 있다 — 방문자 질문 제한과 같은 로직이므로
 * 두 벌로 두지 않는다.
 */
@Component
public class SignupRateLimiter {

    private static final int MAX_ATTEMPTS = 5;

    private final SlidingWindowLimiter limiter = new SlidingWindowLimiter(Duration.ofHours(1));

    /** @return 허용되면 true. 초과면 false */
    public boolean tryAcquire(String key) {
        return limiter.tryAcquire(key, MAX_ATTEMPTS);
    }
}
