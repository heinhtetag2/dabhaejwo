package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 위젯이 실제로 호출됐다는 사실을 남긴다.
 *
 * <p>설치 화면의 "연결 확인됨"이 이 값으로 결정된다. <b>이걸 안 남기면 업체는 붙였는지
 * 아닌지를 화면에서 확인할 수 없다</b> — 실제로 대화가 오가는데도 "아직 호출 없음"이었다.
 *
 * <p>{@code REQUIRES_NEW} 다. 기록은 <b>요청의 성패와 무관해야</b> 한다 —
 * 답변이 원가 상한으로 거절돼 롤백돼도 "설치는 됐다"는 사실까지 되감기면 안 된다.
 * 반대로 이 기록이 실패해도 방문자 요청을 막지 않는다.
 */
@Service
public class OriginCallRecorder {

    private static final Logger log = LoggerFactory.getLogger(OriginCallRecorder.class);

    /** 갱신 간격. 화면이 날짜만 보여주므로 분 단위 정밀도가 필요 없다. */
    private static final int THROTTLE_MINUTES = 10;

    private final AllowedOriginRepository originRepository;

    public OriginCallRecorder(AllowedOriginRepository originRepository) {
        this.originRepository = originRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID botId, String origin) {
        if (origin == null || origin.isBlank()) {
            return;
        }
        try {
            OffsetDateTime now = OffsetDateTime.now();
            int updated = originRepository.markCalled(
                    botId, origin, now, now.minusMinutes(THROTTLE_MINUTES));
            log.debug("호출 기록 — bot={}, origin={}, updated={}", botId, origin, updated);
        } catch (RuntimeException e) {
            // 통계용 기록이다. 여기서 터져 방문자 질문이 막히면 배보다 배꼽이 크다.
            log.warn("호출 기록에 실패했습니다 — bot={}, origin={}", botId, origin, e);
        }
    }
}
