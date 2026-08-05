package com.dabhaejwo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 활성화.
 *
 * <p>지금 도는 것은 일 집계 배치 하나다({@code DailyUsageAggregator}).
 *
 * <p>인스턴스가 여러 대가 되면 같은 배치가 동시에 돈다. 지금은 다시 계산해 덮는
 * 방식이라 결과가 같아 문제가 없지만, 알림 발송처럼 <b>부작용이 있는 작업</b>을
 * 여기 붙일 때는 잠금이 필요해진다 (docs/IMPROVEMENTS.md 등록).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
