package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.guard.repository.CostGuardRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.dto.response.TenantSummaryResponse;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.usage.entity.CostRatio;
import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;
import com.dabhaejwo.global.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 업체 목록 조회.
 *
 * <p>원가율은 tenant_daily_usage 한 번의 질의로 전 업체를 집계해 계산한다.
 * 업체마다 ai_usage 를 조회하면 업체 수에 비례해 느려진다.
 */
@Service
public class TenantQueryService {

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final TenantDailyUsageRepository dailyUsageRepository;
    private final CostGuardRepository costGuardRepository;

    public TenantQueryService(TenantRepository tenantRepository,
                              PlanRepository planRepository,
                              TenantDailyUsageRepository dailyUsageRepository,
                              CostGuardRepository costGuardRepository) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.costGuardRepository = costGuardRepository;
    }

    /**
     * @param sort 기본은 원가율 내림차순 — 운영자가 매일 가장 먼저 확인해야 할 대상이 손실 계정이다.
     */
    @Transactional(readOnly = true)
    public PageResponse<TenantSummaryResponse> list(String query, TenantSort sort, int page, Integer size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size));
        // 기본 목록은 해지 업체를 제외한다 (tenant-plan.md §4.1.1).
        Page<Tenant> tenants = tenantRepository.search(
                (query == null || query.isBlank()) ? null : query.strip(),
                TenantStatus.CHURNED,
                pageable);

        Map<UUID, Plan> plans = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));
        Map<UUID, TenantDailyUsageRepository.MonthlyTotal> totals = currentMonthTotals();
        int warnThreshold = costGuardRepository.current().getCostRatioWarnPercent();

        List<TenantSummaryResponse> content = tenants.getContent().stream()
                .map(tenant -> toSummary(tenant, plans, totals, warnThreshold))
                .sorted(sort.comparator())
                .toList();

        return new PageResponse<>(content, new PageResponse.PageInfo(
                tenants.getNumber(), tenants.getSize(),
                tenants.getTotalElements(), tenants.getTotalPages()));
    }

    private Map<UUID, TenantDailyUsageRepository.MonthlyTotal> currentMonthTotals() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.withDayOfMonth(1);
        return dailyUsageRepository.aggregateBetween(from, today).stream()
                .collect(Collectors.toMap(
                        TenantDailyUsageRepository.MonthlyTotal::getTenantId,
                        Function.identity()));
    }

    private TenantSummaryResponse toSummary(Tenant tenant,
                                            Map<UUID, Plan> plans,
                                            Map<UUID, TenantDailyUsageRepository.MonthlyTotal> totals,
                                            int warnThreshold) {
        Plan plan = plans.get(tenant.getPlanId());
        TenantDailyUsageRepository.MonthlyTotal total = totals.get(tenant.getId());

        BigDecimal cost = total == null ? BigDecimal.ZERO : total.getCostKrw();
        long convCount = total == null ? 0L : total.getConvCount();
        int billed = plan == null ? 0 : plan.getMonthlyFee();
        CostRatio ratio = CostRatio.of(cost, billed, warnThreshold);

        return new TenantSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getPrimaryDomain(),
                tenant.getStatus(),
                plan == null ? null : new TenantSummaryResponse.PlanRef(plan.getId(), plan.getName()),
                convCount,
                plan == null ? 0 : plan.getConvLimit(),
                cost,
                billed,
                ratio.percent());
    }

    public enum TenantSort {

        COST_RATIO_DESC(Comparator.comparingInt(TenantSummaryResponse::costRatioPercent).reversed()),
        NAME_ASC(Comparator.comparing(TenantSummaryResponse::name)),
        CONV_DESC(Comparator.comparingLong(TenantSummaryResponse::convCount).reversed());

        private final Comparator<TenantSummaryResponse> comparator;

        TenantSort(Comparator<TenantSummaryResponse> comparator) {
            this.comparator = comparator;
        }

        public Comparator<TenantSummaryResponse> comparator() {
            return comparator;
        }
    }
}
