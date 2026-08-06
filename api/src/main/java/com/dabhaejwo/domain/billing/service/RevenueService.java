package com.dabhaejwo.domain.billing.service;

import com.dabhaejwo.domain.billing.dto.response.BillingRecordResponse;
import com.dabhaejwo.domain.billing.dto.response.MonthlyRevenueResponse;
import com.dabhaejwo.domain.billing.dto.response.RevenueSummaryResponse;
import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.entity.BillingStatus;
import com.dabhaejwo.domain.billing.repository.BillingRecordRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.tenant.service.TenantQueryService;
import com.dabhaejwo.domain.usage.repository.AiUsageRepository;
import com.dabhaejwo.global.common.BusinessDay;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 정산 — <b>실제로 오간 돈</b>.
 *
 * <p>수익성({@code ProfitabilityService})이 원가를 보는 화면이라면 여기는 청구·수납·미수를
 * 본다. 두 화면이 다른 질문에 답한다:
 * <ul>
 *   <li>수익성 — 이 업체가 쓰는 만큼 값을 받고 있나 (원가 ÷ <b>정가</b>)
 *   <li>정산 — 우리가 이번 달에 실제로 얼마를 받았나 ({@code billing_records})
 * </ul>
 *
 * <p><b>이 서비스는 읽기만 한다.</b> {@code billing_records} 가 원장이고, 그것을 쓰는 곳은
 * {@link BillingService} 하나다. 조회 화면이 원장을 고칠 수 있게 되면 "이 금액이 왜 이런가"의
 * 답이 사라진다.
 */
@Service
public class RevenueService {

    /** 월별 추이 기본 개월 수. */
    private static final int DEFAULT_MONTHS = 12;
    private static final int MAX_MONTHS = 36;

    private final BillingRecordRepository recordRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final AiUsageRepository aiUsageRepository;
    private final TenantQueryService tenantQueryService;

    public RevenueService(BillingRecordRepository recordRepository,
                          TenantRepository tenantRepository,
                          PlanRepository planRepository,
                          AiUsageRepository aiUsageRepository,
                          TenantQueryService tenantQueryService) {
        this.recordRepository = recordRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.aiUsageRepository = aiUsageRepository;
        this.tenantQueryService = tenantQueryService;
    }

    @Transactional(readOnly = true)
    public RevenueSummaryResponse summary() {
        YearMonth month = YearMonth.from(BusinessDay.today());
        LocalDate from = month.atDay(1);

        BillingRecordRepository.PeriodTotal total = recordRepository
                .aggregateByPeriod(from, from.plusMonths(1)).stream()
                .findFirst()
                .orElse(null);

        long billed = total == null ? 0 : total.getBilledKrw();
        long collected = total == null ? 0 : total.getCollectedKrw();
        long refunded = total == null ? 0 : total.getRefundedKrw();
        long paidCount = total == null ? 0 : total.getPaidCount();
        long unpaidCount = total == null ? 0 : total.getFailedCount() + total.getPendingCount();

        BigDecimal modelCost = monthlyCosts(month, month).getOrDefault(month, BigDecimal.ZERO);
        TrialCost trial = trialCost(month);

        return new RevenueSummaryResponse(
                month.toString(),
                // MRR 은 업체 목록·오늘 화면과 같은 계산을 쓴다. 두 화면이 다른 MRR 을
                // 보여주면 어느 쪽도 믿을 수 없다.
                tenantQueryService.revenue().mrrKrw(),
                billed,
                collected,
                refunded,
                RevenueMath.outstanding(billed, collected, refunded),
                paidCount,
                unpaidCount,
                modelCost,
                RevenueMath.margin(collected, modelCost),
                RevenueMath.marginPercent(collected, modelCost),
                trial.costKrw(),
                trial.tenantCount());
    }

    /**
     * 월별 추이. 최신 달이 앞에 온다.
     *
     * <p>데이터가 없는 달도 0 으로 채워 내려보낸다 — 빠진 달이 있으면 추이가 실제보다
     * 좋아 보인다(매출 없던 달이 그래프에서 사라진다).
     */
    @Transactional(readOnly = true)
    public List<MonthlyRevenueResponse> monthly(int months) {
        int span = Math.min(Math.max(months, 1), MAX_MONTHS);
        YearMonth today = YearMonth.from(BusinessDay.today());
        YearMonth oldest = today.minusMonths(span - 1L);
        LocalDate from = oldest.atDay(1);
        OffsetDateTime fromAt = BusinessDay.startOf(from);

        Map<YearMonth, BillingRecordRepository.PeriodTotal> billing =
                recordRepository.aggregateByPeriod(from, today.plusMonths(1).atDay(1)).stream()
                        .collect(Collectors.toMap(
                                row -> YearMonth.from(row.getPeriod()), Function.identity()));

        Map<YearMonth, BigDecimal> costs = monthlyCosts(oldest, today);
        Map<YearMonth, Long> signups = counts(tenantRepository.countSignupsByMonth(fromAt));
        Map<YearMonth, Long> churns = counts(tenantRepository.countChurnsByMonth(fromAt));
        Map<YearMonth, Long> converted = convertedByCohort(fromAt);

        LocalDate now = BusinessDay.today();
        List<MonthlyRevenueResponse> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            // 최신이 앞이다.
            YearMonth month = today.minusMonths(i);
            BillingRecordRepository.PeriodTotal row = billing.get(month);
            BigDecimal cost = costs.getOrDefault(month, BigDecimal.ZERO);
            long collected = row == null ? 0 : row.getCollectedKrw();
            long signupCount = signups.getOrDefault(month, 0L);
            long convertedCount = converted.getOrDefault(month, 0L);

            result.add(new MonthlyRevenueResponse(
                    month.toString(),
                    row == null ? 0 : row.getBilledKrw(),
                    collected,
                    row == null ? 0 : row.getRefundedKrw(),
                    row == null ? 0 : row.getFailedCount(),
                    cost,
                    RevenueMath.margin(collected, cost),
                    signupCount,
                    convertedCount,
                    RevenueMath.conversionPercent(signupCount, convertedCount),
                    churns.getOrDefault(month, 0L),
                    RevenueMath.cohortOpen(month, now)));
        }
        return result;
    }

    /**
     * 청구 목록.
     *
     * <p>정렬은 <b>금액 큰 순</b>이다. 미수를 쫓는 것이 이 목록의 용도라, 같은 미수라면
     * 큰 건부터 확인하는 편이 낫다.
     */
    @Transactional(readOnly = true)
    public PageResponse<BillingRecordResponse> records(String periodMonth,
                                                       BillingStatus status,
                                                       int page,
                                                       Integer size) {
        LocalDate period = parsePeriod(periodMonth);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size),
                Sort.by(Sort.Direction.DESC, "amount").and(Sort.by(Sort.Direction.ASC, "id")));

        Page<BillingRecord> found = status == null
                ? recordRepository.findAllByPeriod(period, pageable)
                : recordRepository.findAllByPeriodAndStatus(period, status, pageable);

        Map<UUID, Tenant> tenants = tenantRepository
                .findAllById(found.getContent().stream().map(BillingRecord::getTenantId).toList())
                .stream()
                .collect(Collectors.toMap(Tenant::getId, Function.identity()));
        Map<UUID, Plan> plans = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));

        return PageResponse.of(found, record -> {
            Tenant tenant = tenants.get(record.getTenantId());
            Plan plan = tenant == null ? null : plans.get(tenant.getPlanId());
            return BillingRecordResponse.of(
                    record,
                    // 해지 후 정리된 업체의 청구 기록은 남는다 — 원장이라 지우지 않는다.
                    tenant == null ? "(삭제된 업체)" : tenant.getName(),
                    plan == null ? null : plan.getName());
        });
    }

    /**
     * 체험 업체가 이번 달에 태운 모델 원가.
     *
     * <p>매출이 0원이라 <b>전액이 손실</b>인데 원가율로는 드러나지 않는다 — 정가가 0이라
     * 나눌 수 없어 {@code CostRatio} 가 0%/정상을 돌려주기 때문이다. 체험이 늘수록
     * 커지는 유일한 지표라 따로 센다.
     */
    private TrialCost trialCost(YearMonth month) {
        List<UUID> trialTenants = tenantRepository.findAllByStatus(TenantStatus.TRIAL).stream()
                .map(Tenant::getId)
                .toList();
        if (trialTenants.isEmpty()) {
            // JPQL 의 IN () 은 문법 오류다. 부르지 않는다.
            return new TrialCost(BigDecimal.ZERO, 0);
        }
        BigDecimal cost = aiUsageRepository.sumCostKrwByTenants(
                utcStartOf(month), utcStartOf(month.plusMonths(1)), trialTenants);
        return new TrialCost(cost == null ? BigDecimal.ZERO : cost, trialTenants.size());
    }

    private record TrialCost(BigDecimal costKrw, long tenantCount) {
    }

    /**
     * 월별 모델 원가.
     *
     * <p>경계가 <b>UTC</b> 인 것은 AI 사용량 화면과 같은 숫자를 내기 위해서다
     * ({@code AiUsageRepository#aggregateMonthlyCost} 주석 참조).
     */
    private Map<YearMonth, BigDecimal> monthlyCosts(YearMonth oldest, YearMonth newest) {
        Map<YearMonth, BigDecimal> result = new HashMap<>();
        for (AiUsageRepository.MonthlyCost row : aiUsageRepository.aggregateMonthlyCost(
                utcStartOf(oldest), utcStartOf(newest.plusMonths(1)))) {
            result.put(YearMonth.from(row.getMonth()), row.getCostKrw());
        }
        return result;
    }

    private OffsetDateTime utcStartOf(YearMonth month) {
        return month.atDay(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
    }

    /**
     * 가입월별 유료 전환 수.
     *
     * <p>"그 달에 가입한 업체 중 <b>지금까지</b> 한 번이라도 결제한 곳"이다. 첫 결제가
     * 언제였는지는 묻지 않는다 — 체험 14일이 걸쳐 있어 가입월과 첫 결제월이 대부분 다르다.
     */
    private Map<YearMonth, Long> convertedByCohort(OffsetDateTime from) {
        Set<UUID> everPaid = new HashSet<>(recordRepository.findTenantIdsEverPaid());
        if (everPaid.isEmpty()) {
            return Map.of();
        }
        Map<YearMonth, Long> result = new HashMap<>();
        for (TenantRepository.SignupCohort row : tenantRepository.findSignupCohorts(from)) {
            if (everPaid.contains(row.getTenantId())) {
                result.merge(YearMonth.from(row.getMonth()), 1L, Long::sum);
            }
        }
        return result;
    }

    private Map<YearMonth, Long> counts(List<TenantRepository.MonthlyCount> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> YearMonth.from(row.getMonth()),
                TenantRepository.MonthlyCount::getTenantCount));
    }

    /**
     * {@code YYYY-MM} → 그 달 1일. 생략하면 이번 달이다.
     *
     * <p>형식이 틀리면 <b>거절한다.</b> 조용히 이번 달로 떨어뜨리면 운영자는 7월을 보고
     * 있다고 믿으면서 8월 숫자를 읽는다 — 화면 어디에도 틀렸다는 표시가 없다.
     */
    private LocalDate parsePeriod(String periodMonth) {
        if (periodMonth == null || periodMonth.isBlank()) {
            return BusinessDay.today().withDayOfMonth(1);
        }
        try {
            return YearMonth.parse(periodMonth.strip()).atDay(1);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "청구월은 YYYY-MM 형식이어야 합니다");
        }
    }

    public static int defaultMonths() {
        return DEFAULT_MONTHS;
    }
}
