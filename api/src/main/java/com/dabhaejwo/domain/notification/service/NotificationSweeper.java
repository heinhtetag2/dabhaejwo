package com.dabhaejwo.domain.notification.service;

import com.dabhaejwo.domain.gap.entity.GapStatus;
import com.dabhaejwo.domain.gap.repository.AnswerGapRepository;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 사건이 없어도 알아야 하는 것들을 주기적으로 찾아 알린다.
 *
 * <p>가입·문의·대리 접속은 <b>순간</b>이 있어 그 자리에서 알리면 된다. 하지만
 * "한도의 80% 를 썼다"·"체험이 3일 남았다"·"원가가 매출을 넘어섰다"는 어떤 순간에도
 * 일어나지 않는다 — 시간이 지나면서 <b>사실이 되어 있을 뿐</b>이다. 그래서 훑는다.
 *
 * <p>중복은 발행 시점의 {@code dedupeKey} 가 막는다. 매시 돌아도 같은 알림은 한 번만 남는다 —
 * 이 클래스가 "이미 보냈나"를 따로 기억하지 않아도 되는 이유다.
 *
 * <p><b>인스턴스가 여럿이면 같은 시각에 함께 돈다.</b> 그래도 결과는 같다 —
 * 유니크 인덱스가 두 번째를 떨어뜨린다. 분산 락은 두지 않았다 (IMPROVEMENTS P2).
 */
@Service
public class NotificationSweeper {

    private static final Logger log = LoggerFactory.getLogger(NotificationSweeper.class);

    /** 대화 한도 경고 지점. 80% 에서 한 번, 다 쓰면 또 한 번. */
    private static final int QUOTA_WARN_PERCENT = 80;

    /** 체험 종료를 며칠 전에 알릴지. 하루 전은 이미 늦고, 일주일 전은 잊는다. */
    private static final int TRIAL_NOTICE_DAYS = 3;

    /** 이만큼 쌓이면 업체가 손볼 만한 양이다. 매번 알리면 알림이 무의미해진다. */
    private static final int GAP_THRESHOLD = 10;

    /** 학습 실패가 이만큼이면 우리 쪽 문제일 가능성이 높다. 한두 건은 깨진 파일이다. */
    private static final int INDEXING_FAILURE_THRESHOLD = 5;

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final TenantDailyUsageRepository dailyUsageRepository;
    private final AnswerGapRepository gapRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final NotificationEvents events;

    public NotificationSweeper(TenantRepository tenantRepository,
                               PlanRepository planRepository,
                               TenantDailyUsageRepository dailyUsageRepository,
                               AnswerGapRepository gapRepository,
                               KnowledgeDocumentRepository documentRepository,
                               NotificationEvents events) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.gapRepository = gapRepository;
        this.documentRepository = documentRepository;
        this.events = events;
    }

    /**
     * 매시 20분. 일 집계({@code DailyUsageAggregator})가 정각에 돌므로 그 뒤를 본다 —
     * 같은 시각에 돌면 방금 지난 한 시간의 사용량이 빠진 채로 판단하게 된다.
     */
    @Scheduled(cron = "0 20 * * * *")
    public void runHourly() {
        try {
            sweep();
        } catch (RuntimeException e) {
            // 알림 훑기가 실패해도 서비스는 계속 돈다. 다음 시간에 다시 시도한다.
            log.error("알림 훑기에 실패했습니다", e);
        }
    }

    /**
     * 트랜잭션을 하나로 묶지 않는다. 훑기는 <b>읽고 판단해 알리는</b> 일이라 원자성이 필요 없고,
     * 업체 수만큼 커지는 트랜잭션을 열어두면 그 시간 내내 커넥션을 쥐고 있게 된다.
     * 발행 하나하나는 {@code NotificationPublisher} 가 각자의 트랜잭션으로 처리한다.
     */
    public void sweep() {
        List<Tenant> tenants = activeTenants();
        if (tenants.isEmpty()) {
            return;
        }

        Map<UUID, Plan> plans = new HashMap<>();
        planRepository.findAll().forEach(plan -> plans.put(plan.getId(), plan));

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        Map<UUID, TenantDailyUsageRepository.MonthlyTotal> monthly = new HashMap<>();
        dailyUsageRepository.aggregateBetween(monthStart, today)
                .forEach(row -> monthly.put(row.getTenantId(), row));

        for (Tenant tenant : tenants) {
            Plan plan = plans.get(tenant.getPlanId());
            if (plan == null) {
                continue;
            }
            TenantDailyUsageRepository.MonthlyTotal usage = monthly.get(tenant.getId());
            checkQuota(tenant, plan, usage);
            checkProfitability(tenant, plan, usage);
            checkTrial(tenant, today);
            checkAnswerGaps(tenant);
        }

        checkIndexingFailures();
    }

    /** 해지된 업체는 볼 것도 알릴 것도 없다. 정지는 알려야 한다 — 풀리면 바로 쓴다. */
    private List<Tenant> activeTenants() {
        List<Tenant> tenants = new java.util.ArrayList<>(
                tenantRepository.findAllByStatus(TenantStatus.ACTIVE));
        tenants.addAll(tenantRepository.findAllByStatus(TenantStatus.TRIAL));
        tenants.addAll(tenantRepository.findAllByStatus(TenantStatus.SUSPENDED));
        return tenants;
    }

    /**
     * 이번 달 대화 한도.
     *
     * <p>100% 와 80% 는 <b>다른 알림</b>이다. 하나로 합치면 "다 썼다"가 "곧 다 쓴다"의
     * 중복으로 걸러져 정작 멈춘 순간을 못 알린다.
     */
    private void checkQuota(Tenant tenant, Plan plan, TenantDailyUsageRepository.MonthlyTotal usage) {
        if (plan.getConvLimit() <= 0) {
            return;
        }
        long used = usage == null ? 0L : usage.getConvCount();
        int percent = (int) (used * 100 / plan.getConvLimit());

        if (percent >= 100) {
            events.quotaWarning(tenant.getId(), 100, used, plan.getConvLimit());
        } else if (percent >= QUOTA_WARN_PERCENT) {
            events.quotaWarning(tenant.getId(), QUOTA_WARN_PERCENT, used, plan.getConvLimit());
        }
    }

    /**
     * 원가가 월 요금을 넘었다 — 쓸수록 적자인 업체다 (admin-console-plan.md §5).
     *
     * <p>체험 업체는 제외한다. 요금이 0원이라 원가율이 무한대가 되고,
     * "체험 중인데 적자다"는 알림은 매일 전 체험 업체에서 울린다.
     */
    private void checkProfitability(Tenant tenant, Plan plan,
                                    TenantDailyUsageRepository.MonthlyTotal usage) {
        if (usage == null || plan.getMonthlyFee() <= 0) {
            return;
        }
        BigDecimal fee = BigDecimal.valueOf(plan.getMonthlyFee());
        if (usage.getCostKrw().compareTo(fee) <= 0) {
            return;
        }
        int ratio = usage.getCostKrw()
                .multiply(BigDecimal.valueOf(100))
                .divide(fee, 0, RoundingMode.HALF_UP)
                .intValue();
        events.tenantCostExceeded(tenant.getId(), tenant.getName(), ratio);
    }

    /** 체험 종료. 업체와 운영자 <b>둘 다</b> 알아야 한다 — 한쪽만 알면 전환이 흐른다. */
    private void checkTrial(Tenant tenant, LocalDate today) {
        if (tenant.getStatus() != TenantStatus.TRIAL || tenant.getTrialEndsAt() == null) {
            return;
        }
        long daysLeft = ChronoUnit.DAYS.between(
                today, tenant.getTrialEndsAt().toLocalDate());
        if (daysLeft < 0 || daysLeft > TRIAL_NOTICE_DAYS) {
            return;
        }
        int left = (int) daysLeft;
        events.trialEndingForTenant(tenant.getId(), left);
        events.trialEndingForOps(tenant.getId(), tenant.getName(), left);
    }

    private void checkAnswerGaps(Tenant tenant) {
        // 업체에 보내는 알림이다. 서비스를 가리지 않고 전부 센다.
        long open = gapRepository.countAcrossBotsAndStatus(tenant.getId(), GapStatus.OPEN);
        if (open >= GAP_THRESHOLD) {
            events.answerGapsPiling(tenant.getId(), open);
        }
    }

    /**
     * 학습 실패 누적. 업체별이 아니라 <b>전체</b>로 본다 —
     * 한 업체에서 5건 실패한 것과 다섯 업체에서 1건씩 실패한 것은 우리 쪽 문제라는 점에서 같다.
     */
    private void checkIndexingFailures() {
        long failed = documentRepository.countByStatus(DocumentStatus.FAILED);
        if (failed >= INDEXING_FAILURE_THRESHOLD) {
            events.indexingFailuresForOps(failed);
        }
    }
}
