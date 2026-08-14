package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.billing.repository.BillingRecordRepository;
import com.dabhaejwo.domain.faq.repository.FaqRepository;
import com.dabhaejwo.domain.guard.repository.CostGuardRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.dto.response.TenantDetailResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantFilterCountsResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantSummaryResponse;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.QuotaOverrideRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.usage.entity.CostRatio;
import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.tenant.dto.response.OpsBotResponse;
import com.dabhaejwo.domain.tenant.dto.response.AllowedOriginResponse;
import com.dabhaejwo.domain.tenant.repository.AllowedOriginRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 업체 조회 — 목록·상세·필터 건수.
 *
 * <p>원가율은 {@code tenant_daily_usage} 한 번의 질의로 전 업체를 집계해 계산한다.
 * 업체마다 {@code ai_usage} 를 조회하면 업체 수에 비례해 느려진다
 * (admin-console-plan.md §6.1).
 */
@Service
public class TenantQueryService {

    /** "미접속" 판정 기준. 필터 칩 이름(7일 미접속)과 같은 값이어야 한다. */
    private static final int INACTIVE_DAYS = 7;

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final TenantDailyUsageRepository dailyUsageRepository;
    private final CostGuardRepository costGuardRepository;
    private final com.dabhaejwo.domain.bot.repository.BotRepository botRepository;
    private final AllowedOriginRepository allowedOriginRepository;
    private final TenantMemberRepository memberRepository;
    private final BillingRecordRepository billingRecordRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final FaqRepository faqRepository;
    private final QuotaOverrideRepository quotaOverrideRepository;

    public TenantQueryService(TenantRepository tenantRepository,
                              PlanRepository planRepository,
                              TenantDailyUsageRepository dailyUsageRepository,
                              CostGuardRepository costGuardRepository,
                              TenantMemberRepository memberRepository,
                              BillingRecordRepository billingRecordRepository,
                              KnowledgeDocumentRepository documentRepository,
                              FaqRepository faqRepository,
                              QuotaOverrideRepository quotaOverrideRepository,
                              com.dabhaejwo.domain.bot.repository.BotRepository botRepository,
                              AllowedOriginRepository allowedOriginRepository) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.costGuardRepository = costGuardRepository;
        this.botRepository = botRepository;
        this.allowedOriginRepository = allowedOriginRepository;
        this.memberRepository = memberRepository;
        this.billingRecordRepository = billingRecordRepository;
        this.documentRepository = documentRepository;
        this.faqRepository = faqRepository;
        this.quotaOverrideRepository = quotaOverrideRepository;
    }

    /**
     * @param sort 기본은 원가율 내림차순 — 운영자가 매일 가장 먼저 확인해야 할 대상이 손실 계정이다.
     */
    @Transactional(readOnly = true)
    public PageResponse<TenantSummaryResponse> list(String query,
                                                    TenantFilter filter,
                                                    TenantSort sort,
                                                    int page,
                                                    Integer size) {
        List<TenantSummaryResponse> all = candidates(query).stream()
                .filter(candidate -> matches(filter, candidate))
                .map(Candidate::summary)
                .sorted(sort.comparator())
                .toList();

        int pageSize = PageResponse.clampSize(size);
        int pageNumber = Math.max(page, 0);
        int from = Math.min(pageNumber * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());

        return new PageResponse<>(
                List.copyOf(all.subList(from, to)),
                new PageResponse.PageInfo(pageNumber, pageSize, all.size(),
                        (int) Math.ceil((double) all.size() / pageSize)));
    }

    @Transactional(readOnly = true)
    public TenantFilterCountsResponse filterCounts() {
        List<Candidate> candidates = candidates(null);
        return new TenantFilterCountsResponse(
                count(candidates, TenantFilter.ALL),
                count(candidates, TenantFilter.TRIAL),
                count(candidates, TenantFilter.PAYMENT_FAILED),
                count(candidates, TenantFilter.COST_EXCEEDED),
                count(candidates, TenantFilter.INACTIVE_7D),
                count(candidates, TenantFilter.SUSPENDED),
                count(candidates, TenantFilter.CHURNED));
    }

    @Transactional(readOnly = true)
    public TenantDetailResponse detail(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        TenantMetrics metrics = TenantMetrics.from(
                dailyUsageRepository.aggregateBetween(monthStart, today).stream()
                        .filter(total -> tenantId.equals(total.getTenantId()))
                        .findFirst()
                        .orElse(null));

        QuotaOverrideRepository.Delta delta = quotaOverrideRepository.sumDelta(tenantId, monthStart);
        int billed = plan.getMonthlyFee();
        CostRatio ratio = CostRatio.of(metrics.costKrw(), billed,
                costGuardRepository.current().getCostRatioWarnPercent());

        OffsetDateTime lastSeenAt = memberRepository.findLastSeenByTenant().stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .map(TenantMemberRepository.LastSeen::getLastSeenAt)
                .findFirst()
                .orElse(null);

        return new TenantDetailResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getPrimaryDomain(),
                tenant.getPublishableKey(),
                tenant.getStatus(),
                tenant.getCurrency(),
                new TenantDetailResponse.PlanDetail(plan.getId(), plan.getName(), plan.getMonthlyFee()),
                metrics.convCount(),
                // 쿼터 증량은 요금제 한도를 고치지 않고 이번 달에만 더해진다.
                plan.getConvLimit() + delta.getConvDelta(),
                documentRepository.countActiveAcrossBots(tenantId),
                plan.getDocLimit() + delta.getDocDelta(),
                // 운영 콘솔은 업체를 본다 — 서비스가 여럿이면 전부 더한다.
                faqRepository.countAcrossBots(tenantId),
                metrics.savedAnswerPercent(),
                metrics.costKrw(),
                billed,
                ratio.percent(),
                tenant.getCreatedAt().toLocalDate(),
                tenant.getTrialEndsAt(),
                tenant.getNextBillingDate(),
                lastSeenAt);
    }

    /**
     * 이번 달 원가 상위 업체. "오늘" 화면의 조치 목록이 쓴다.
     *
     * @param minRatioPercent 이 값 이상인 업체만. 100 이면 손실 계정이다.
     */
    @Transactional(readOnly = true)
    public List<TenantSummaryResponse> costExceeded(int minRatioPercent) {
        return candidates(null).stream()
                .map(Candidate::summary)
                .filter(summary -> summary.status() != TenantStatus.CHURNED)
                .filter(summary -> summary.costRatioPercent() >= minRatioPercent)
                .sorted(TenantSort.COST_RATIO_DESC.comparator())
                .toList();
    }

    /** 가장 최근 결제 시도가 실패한 업체. "오늘" 화면의 조치 목록이 쓴다. */
    @Transactional(readOnly = true)
    public List<TenantSummaryResponse> paymentFailed() {
        return candidates(null).stream()
                .filter(candidate -> matches(TenantFilter.PAYMENT_FAILED, candidate))
                .map(Candidate::summary)
                .toList();
    }

    /** 유료로 돌고 있는 업체 수와 월 반복 매출. 체험·정지·해지는 빠진다. */
    @Transactional(readOnly = true)
    public Revenue revenue() {
        List<TenantSummaryResponse> paying = candidates(null).stream()
                .map(Candidate::summary)
                .filter(summary -> summary.status() == TenantStatus.ACTIVE)
                .toList();
        return new Revenue(paying.size(), paying.stream().mapToLong(TenantSummaryResponse::billedKrw).sum());
    }

    public record Revenue(long payingTenantCount, long mrrKrw) {
    }

    /** 해지되지 않은 전체 업체 수. 헤드라인의 분모다. */
    @Transactional(readOnly = true)
    public long activeTenantCount() {
        return candidates(null).stream()
                .filter(candidate -> matches(TenantFilter.ALL, candidate))
                .count();
    }

    /** 체험 종료가 {@code withinDays} 안으로 다가온 업체. */
    @Transactional(readOnly = true)
    public List<Tenant> trialEndingSoon(int withinDays) {
        OffsetDateTime limit = OffsetDateTime.now().plusDays(withinDays);
        return tenantRepository.findAllByStatus(TenantStatus.TRIAL).stream()
                .filter(tenant -> tenant.getTrialEndsAt() != null)
                .filter(tenant -> tenant.getTrialEndsAt().isBefore(limit))
                .sorted(Comparator.comparing(Tenant::getTrialEndsAt))
                .toList();
    }

    /**
     * 업체가 가진 서비스 전부. 운영 콘솔 업체 상세가 쓴다.
     *
     * <p>허용 주소를 함께 준다 — CS 문의 1순위가 "코드 붙였는데 안 떠요"이고 원인 대부분이
     * 미등록 주소인데, 지금까지 그걸 보려면 <b>사유를 적고 대리 로그인을 해야 했다.</b>
     */
    @Transactional(readOnly = true)
    public List<OpsBotResponse> bots(UUID tenantId) {
        return botRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(bot -> OpsBotResponse.of(bot,
                        allowedOriginRepository.findAllByBotId(bot.getId()).stream()
                                .map(AllowedOriginResponse::from)
                                .toList()))
                .toList();
    }

    /**
     * 전 업체를 한 번에 계산한다. 목록·필터 건수·오늘 화면이 같은 계산을 쓰므로 한 곳에 둔다 —
     * 두 벌로 나뉘면 칩 건수와 실제 목록 길이가 어긋나는 날이 온다.
     */
    private List<Candidate> candidates(String query) {
        // 검색어가 없으면 빈 문자열이다. null 을 넘기면 PostgreSQL 이 bytea 로 바인딩해 터진다
        // (TenantRepository#searchAll 주석 참조).
        String normalized = query == null ? "" : query.strip();
        List<Tenant> tenants = tenantRepository.searchAll(normalized);

        Map<UUID, Plan> plans = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));

        LocalDate today = LocalDate.now();
        Map<UUID, TenantDailyUsageRepository.MonthlyTotal> totals =
                dailyUsageRepository.aggregateBetween(today.withDayOfMonth(1), today).stream()
                        .collect(Collectors.toMap(
                                TenantDailyUsageRepository.MonthlyTotal::getTenantId,
                                Function.identity()));

        Map<UUID, OffsetDateTime> lastSeen = memberRepository.findLastSeenByTenant().stream()
                .collect(Collectors.toMap(
                        TenantMemberRepository.LastSeen::getTenantId,
                        TenantMemberRepository.LastSeen::getLastSeenAt));

        Set<UUID> paymentFailed =
                new HashSet<>(billingRecordRepository.findTenantIdsWithLatestPaymentFailed());
        int warnThreshold = costGuardRepository.current().getCostRatioWarnPercent();

        // 서비스 수를 한 번에 센다. 업체마다 따로 부르면 목록 한 번에 왕복이 업체 수만큼 는다.
        Map<UUID, Long> botCounts = botRepository.findAll().stream()
                .collect(Collectors.groupingBy(Bot::getTenantId, Collectors.counting()));

        return tenants.stream()
                .map(tenant -> {
                    Plan plan = plans.get(tenant.getPlanId());
                    TenantMetrics metrics = TenantMetrics.from(totals.get(tenant.getId()));
                    // 협의가(기업 요금제)는 monthly_fee 가 0 이라 원가율을 정의할 수 없다.
                    // CostRatio 가 0% + NORMAL 로 내려보낸다.
                    int billed = plan == null ? 0 : plan.getMonthlyFee();
                    CostRatio ratio = CostRatio.of(metrics.costKrw(), billed, warnThreshold);

                    TenantSummaryResponse summary = new TenantSummaryResponse(
                            tenant.getId(),
                            tenant.getName(),
                            tenant.getPrimaryDomain(),
                            botCounts.getOrDefault(tenant.getId(), 0L).intValue(),
                            tenant.getStatus(),
                            plan == null ? null
                                    : new TenantSummaryResponse.PlanRef(plan.getId(), plan.getName()),
                            metrics.convCount(),
                            plan == null ? 0 : plan.getConvLimit(),
                            metrics.costKrw(),
                            billed,
                            ratio.percent());

                    return new Candidate(summary, lastSeen.get(tenant.getId()),
                            paymentFailed.contains(tenant.getId()), metrics);
                })
                .toList();
    }

    /**
     * 수익성 화면용 목록. 원가율 내림차순이며 해지 업체는 뺀다.
     *
     * <p>목록 화면과 같은 계산을 쓴다 — 두 화면의 원가율이 다르게 보이면 어느 쪽도 믿을 수 없다.
     */
    @Transactional(readOnly = true)
    public List<TenantProfitability> profitability() {
        return candidates(null).stream()
                .filter(candidate -> candidate.summary().status() != TenantStatus.CHURNED)
                .map(candidate -> new TenantProfitability(
                        candidate.summary(), candidate.metrics().savedAnswerPercent()))
                .sorted(java.util.Comparator.comparingInt(
                        (TenantProfitability item) -> item.summary().costRatioPercent()).reversed())
                .toList();
    }

    /** 수익성 항목. 요약에 저장 답변 비율을 더한 것이다 — 원가율과 나란히 놓으면 상관관계가 눈에 보인다. */
    public record TenantProfitability(TenantSummaryResponse summary, int savedAnswerPercent) {
    }

    /**
     * 필터 판정. 결제 실패와 마지막 접속은 요약 응답에 없는 값이라 여기서 본다 —
     * 화면에 안 쓰는 필드를 응답에 싣지 않기 위해서다.
     */
    private boolean matches(TenantFilter filter, Candidate candidate) {
        TenantSummaryResponse summary = candidate.summary();
        boolean churned = summary.status() == TenantStatus.CHURNED;
        return switch (filter) {
            case ALL -> !churned;
            case TRIAL -> !churned && summary.status() == TenantStatus.TRIAL;
            case SUSPENDED -> summary.status() == TenantStatus.SUSPENDED;
            case CHURNED -> churned;
            case COST_EXCEEDED -> !churned
                    && summary.costRatioPercent() >= CostRatio.LOSS_THRESHOLD_PERCENT;
            case PAYMENT_FAILED -> !churned && candidate.paymentFailed();
            // 한 번도 접속한 적 없는 업체도 미접속이다 — 가입만 하고 사라진 계정이 여기 잡힌다.
            case INACTIVE_7D -> !churned
                    && (candidate.lastSeenAt() == null
                        || candidate.lastSeenAt().isBefore(
                                OffsetDateTime.now().minusDays(INACTIVE_DAYS)));
        };
    }

    private long count(List<Candidate> candidates, TenantFilter filter) {
        return candidates.stream().filter(candidate -> matches(filter, candidate)).count();
    }

    /** 목록 계산 중간값. 필터가 요약에 없는 값도 봐야 한다. */
    private record Candidate(TenantSummaryResponse summary,
                             OffsetDateTime lastSeenAt,
                             boolean paymentFailed,
                             TenantMetrics metrics) {
    }

    public enum TenantSort {

        COST_RATIO_DESC(Comparator.comparingInt(TenantSummaryResponse::costRatioPercent).reversed()
                .thenComparing(TenantSummaryResponse::name)),
        NAME_ASC(Comparator.comparing(TenantSummaryResponse::name)),
        CONV_DESC(Comparator.comparingLong(TenantSummaryResponse::convCount).reversed()
                .thenComparing(TenantSummaryResponse::name));

        private final Comparator<TenantSummaryResponse> comparator;

        TenantSort(Comparator<TenantSummaryResponse> comparator) {
            this.comparator = comparator;
        }

        public Comparator<TenantSummaryResponse> comparator() {
            return comparator;
        }
    }

    /**
     * 필터 칩. 기본(ALL)은 해지 업체를 제외한다 — 해지는 명시적으로 골라야 보인다
     * (admin-console-tenant-plan.md §4.1.1).
     */
    public enum TenantFilter {
        ALL,
        TRIAL,
        PAYMENT_FAILED,
        COST_EXCEEDED,
        INACTIVE_7D,
        SUSPENDED,
        CHURNED
    }
}
