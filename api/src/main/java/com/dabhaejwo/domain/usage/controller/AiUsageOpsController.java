package com.dabhaejwo.domain.usage.controller;

import com.dabhaejwo.domain.usage.dto.response.AiUsageSummaryResponse;
import com.dabhaejwo.domain.usage.dto.response.DailyCostResponse;
import com.dabhaejwo.domain.usage.dto.response.ModelUsageResponse;
import com.dabhaejwo.domain.usage.dto.response.ProfitabilityResponse;
import com.dabhaejwo.domain.usage.dto.response.TopTenantUsageResponse;
import com.dabhaejwo.domain.usage.service.AiUsageQueryService;
import com.dabhaejwo.domain.usage.service.ProfitabilityService;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/ops")
public class AiUsageOpsController {

    /** 14일 추이. 화면 계약이므로 상한을 둔다 — 무제한 기간 조회를 허용하지 않는다. */
    private static final int MAX_DAYS = 90;

    private final AiUsageQueryService usageService;
    private final ProfitabilityService profitabilityService;

    public AiUsageOpsController(AiUsageQueryService usageService,
                                ProfitabilityService profitabilityService) {
        this.usageService = usageService;
        this.profitabilityService = profitabilityService;
    }

    @GetMapping("/profitability")
    @RequirePermission(Permission.PROFITABILITY_READ)
    public ProfitabilityResponse profitability(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(required = false) Integer size) {
        return profitabilityService.list(page, size);
    }

    @GetMapping("/ai-usage/summary")
    @RequirePermission(Permission.AI_USAGE_READ)
    public AiUsageSummaryResponse summary() {
        return usageService.summary();
    }

    @GetMapping("/ai-usage/daily")
    @RequirePermission(Permission.AI_USAGE_READ)
    public List<DailyCostResponse> daily(@RequestParam(defaultValue = "14") int days) {
        return usageService.daily(Math.min(Math.max(days, 1), MAX_DAYS));
    }

    /** {@code periodMonth} 는 {@code 2026-08} 형태다 (api-contracts.md §0-1). */
    @GetMapping("/ai-usage/by-model")
    @RequirePermission(Permission.AI_USAGE_READ)
    public List<ModelUsageResponse> byModel(@RequestParam(required = false) String periodMonth) {
        return usageService.byModel(periodMonth == null || periodMonth.isBlank()
                ? YearMonth.now()
                : YearMonth.parse(periodMonth));
    }

    @GetMapping("/ai-usage/top-tenants")
    @RequirePermission(Permission.AI_USAGE_READ)
    public List<TopTenantUsageResponse> topTenants(@RequestParam(defaultValue = "5") int limit) {
        return usageService.topTenants(Math.min(limit, 50));
    }
}
