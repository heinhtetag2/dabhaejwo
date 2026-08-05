package com.dabhaejwo.domain.usage.service;

import com.dabhaejwo.domain.guard.repository.CostGuardRepository;
import com.dabhaejwo.domain.tenant.dto.response.TenantSummaryResponse;
import com.dabhaejwo.domain.tenant.service.TenantQueryService;
import com.dabhaejwo.domain.usage.dto.response.ProfitabilityResponse;
import com.dabhaejwo.domain.usage.entity.CostRatio;
import com.dabhaejwo.global.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 수익성 — 어느 업체가 돈을 벌어주고 어느 업체가 손해인지.
 *
 * <p>업체 목록과 <b>같은 계산</b>을 쓴다 (TenantQueryService). 두 화면의 원가율이
 * 다르게 보이면 어느 쪽도 믿을 수 없기 때문이다.
 */
@Service
public class ProfitabilityService {

    private final TenantQueryService tenantQueryService;
    private final CostGuardRepository costGuardRepository;

    public ProfitabilityService(TenantQueryService tenantQueryService,
                                CostGuardRepository costGuardRepository) {
        this.tenantQueryService = tenantQueryService;
        this.costGuardRepository = costGuardRepository;
    }

    @Transactional(readOnly = true)
    public ProfitabilityResponse list(int page, Integer size) {
        List<TenantQueryService.TenantProfitability> all = tenantQueryService.profitability();

        long revenue = all.stream().mapToLong(item -> item.summary().billedKrw()).sum();
        BigDecimal cost = all.stream()
                .map(item -> item.summary().costKrw())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long exceeded = all.stream()
                .filter(item -> item.summary().costRatioPercent() >= CostRatio.LOSS_THRESHOLD_PERCENT)
                .count();

        int pageSize = PageResponse.clampSize(size);
        int pageNumber = Math.max(page, 0);
        int from = Math.min(pageNumber * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());

        List<ProfitabilityResponse.Item> content = all.subList(from, to).stream()
                .map(item -> {
                    TenantSummaryResponse summary = item.summary();
                    return new ProfitabilityResponse.Item(
                            new ProfitabilityResponse.TenantRef(summary.id(), summary.name()),
                            summary.plan() == null ? null : summary.plan().name(),
                            summary.billedKrw(),
                            summary.costKrw(),
                            summary.costRatioPercent(),
                            item.savedAnswerPercent());
                })
                .toList();

        return new ProfitabilityResponse(
                new ProfitabilityResponse.Stats(
                        revenue,
                        cost,
                        // 전체 원가율은 업체별 원가율의 평균이 아니라 총원가 ÷ 총매출이다.
                        // 평균을 내면 매출 0원인 체험 업체가 전체를 끌어내린다.
                        percent(cost, revenue),
                        averageSavedAnswerPercent(all),
                        exceeded,
                        costGuardRepository.current().getCostRatioWarnPercent()),
                content,
                new PageResponse.PageInfo(pageNumber, pageSize, all.size(),
                        (int) Math.ceil((double) all.size() / pageSize)));
    }

    private int percent(BigDecimal part, long total) {
        if (total <= 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int averageSavedAnswerPercent(List<TenantQueryService.TenantProfitability> all) {
        if (all.isEmpty()) {
            return 0;
        }
        return (int) Math.round(all.stream()
                .mapToInt(TenantQueryService.TenantProfitability::savedAnswerPercent)
                .average()
                .orElse(0));
    }
}
