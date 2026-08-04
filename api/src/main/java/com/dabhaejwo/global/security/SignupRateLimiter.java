package com.dabhaejwo.global.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 가입 시도 제한.
 *
 * <p>이메일 인증이 없어 가짜 주소로 체험 계정을 반복 생성할 수 있다
 * (tenant-public-plan.md §5.4). 체험 한도와 일일 원가 상한이 피해를 제한하지만,
 * 만들어지는 것 자체를 늦춰 두는 편이 낫다.
 *
 * <p><b>인스턴스 메모리에만 산다.</b> 서버가 여러 대가 되면 대당 한도가 되어 실효가 떨어지고,
 * 재시작하면 초기화된다. 지금은 단일 인스턴스라 충분하지만 확장 시 Redis 로 옮겨야 한다 —
 * docs/IMPROVEMENTS.md 참조.
 */
@Component
public class SignupRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofHours(1);
    /** 방치하면 맵이 무한히 자란다. 정리 없는 캐시는 결국 메모리 누수다. */
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    /** @return 허용되면 true. 초과면 false */
    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);

        if (attempts.size() > MAX_TRACKED_KEYS) {
            attempts.entrySet().removeIf(entry -> {
                Deque<Instant> times = entry.getValue();
                return times.isEmpty() || times.peekLast().isBefore(cutoff);
            });
        }

        Deque<Instant> times = attempts.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
                times.pollFirst();
            }
            if (times.size() >= MAX_ATTEMPTS) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
