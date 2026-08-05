package com.dabhaejwo.domain.usage.service;

import com.dabhaejwo.domain.conversation.repository.ConversationRepository;
import com.dabhaejwo.domain.conversation.repository.MessageRepository;
import com.dabhaejwo.domain.usage.entity.TenantDailyUsage;
import com.dabhaejwo.domain.usage.repository.AiUsageRepository;
import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@code ai_usage} · {@code conversations} · {@code messages} → {@code tenant_daily_usage}.
 *
 * <p>목록과 대시보드가 원장을 직접 집계하면 업체 수와 기간에 비례해 느려진다.
 * 매시 정각에 그날 것을 다시 계산해 덮는다 (admin-console-plan.md §6.1).
 *
 * <p><b>어제 것도 함께 돌린다.</b> 자정 직전에 발생한 사용량이 자정 이후에 적재되면
 * 그날 집계에 빠지는데, 다음 날 집계만 돌리면 그 구멍은 영영 메워지지 않는다.
 *
 * <p>다시 계산해 덮는 방식(idempotent)인 이유는 증분으로 더하면 배치가 두 번 돌거나
 * 중간에 실패했을 때 숫자가 부풀기 때문이다. 원장이 진실이고 이 테이블은 캐시다.
 */
@Service
public class DailyUsageAggregator {

    private static final Logger log = LoggerFactory.getLogger(DailyUsageAggregator.class);

    /** 집계 기준 타임존. 원장 시각은 timestamptz 이고 화면의 "하루"는 UTC 기준이다. */
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    private final AiUsageRepository aiUsageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TenantDailyUsageRepository dailyUsageRepository;

    public DailyUsageAggregator(AiUsageRepository aiUsageRepository,
                                ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                TenantDailyUsageRepository dailyUsageRepository) {
        this.aiUsageRepository = aiUsageRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.dailyUsageRepository = dailyUsageRepository;
    }

    /** 매시 정각. 초·분을 0 으로 두면 여러 인스턴스가 동시에 돌 수 있지만, 덮어쓰기라 결과는 같다. */
    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        LocalDate today = LocalDate.now(ZONE);
        aggregate(today);
        aggregate(today.minusDays(1));
    }

    /**
     * 하루치 재계산. 그 날짜에 활동이 있었던 업체만 행을 만든다 —
     * 전 업체 × 전 날짜로 행을 채우면 대부분이 0 인 표가 된다.
     */
    @Transactional
    public int aggregate(LocalDate day) {
        OffsetDateTime from = day.atStartOfDay().atOffset(ZONE);
        OffsetDateTime to = from.plusDays(1);

        Map<UUID, AiUsageRepository.TenantDayTotal> costs = new HashMap<>();
        aiUsageRepository.aggregateForDay(from, to)
                .forEach(row -> costs.put(row.getTenantId(), row));

        Map<UUID, Long> convCounts = new HashMap<>();
        conversationRepository.countByTenantBetween(from, to)
                .forEach(row -> convCounts.put(row.getTenantId(), row.getCount()));

        Map<UUID, Long> savedCounts = new HashMap<>();
        messageRepository.countSavedByTenantBetween(from, to)
                .forEach(row -> savedCounts.put(row.getTenantId(), row.getCount()));

        Set<UUID> tenantIds = new HashSet<>(costs.keySet());
        tenantIds.addAll(convCounts.keySet());
        tenantIds.addAll(savedCounts.keySet());

        for (UUID tenantId : tenantIds) {
            AiUsageRepository.TenantDayTotal cost = costs.get(tenantId);
            TenantDailyUsage row = dailyUsageRepository.findByTenantIdAndDay(tenantId, day)
                    .orElseGet(() -> TenantDailyUsage.of(tenantId, day));
            row.overwrite(
                    convCounts.getOrDefault(tenantId, 0L),
                    savedCounts.getOrDefault(tenantId, 0L),
                    cost == null ? 0L : cost.getTokensIn(),
                    cost == null ? 0L : cost.getTokensOut(),
                    cost == null ? BigDecimal.ZERO : cost.getCostKrw());
            dailyUsageRepository.save(row);
        }

        if (!tenantIds.isEmpty()) {
            log.info("일 집계 완료 day={} tenants={}", day, tenantIds.size());
        }
        return tenantIds.size();
    }
}
