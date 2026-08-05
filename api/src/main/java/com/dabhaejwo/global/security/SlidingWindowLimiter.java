package com.dabhaejwo.global.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 키별 슬라이딩 윈도 횟수 제한.
 *
 * <p>가입 시도와 방문자 질문이 같은 계산을 두 벌 갖지 않도록 여기 하나만 둔다.
 * 한도와 창 길이는 쓰는 쪽이 정한다 — 가입은 시간당 5회, 질문은 분당 N회로 성격이 다르다.
 *
 * <p><b>인스턴스 메모리에만 산다.</b> 서버가 여러 대가 되면 대당 한도가 되어 실효가 떨어지고,
 * 재시작하면 초기화된다. 지금은 단일 인스턴스라 충분하지만 확장 시 Redis 로 옮겨야 한다 —
 * docs/IMPROVEMENTS.md 참조.
 */
public class SlidingWindowLimiter {

    /** 방치하면 맵이 무한히 자란다. 정리 없는 캐시는 결국 메모리 누수다. */
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final Duration window;

    public SlidingWindowLimiter(Duration window) {
        this.window = window;
    }

    /**
     * @param maxHits 창 안에서 허용할 횟수. 호출마다 받는 이유는 설정값(DB)이 바뀔 수 있어서다
     * @return 허용되면 true, 초과면 false
     */
    public boolean tryAcquire(String key, int maxHits) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        if (hits.size() > MAX_TRACKED_KEYS) {
            hits.entrySet().removeIf(entry -> {
                Deque<Instant> times = entry.getValue();
                return times.isEmpty() || times.peekLast().isBefore(cutoff);
            });
        }

        Deque<Instant> times = hits.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
                times.pollFirst();
            }
            if (times.size() >= maxHits) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
