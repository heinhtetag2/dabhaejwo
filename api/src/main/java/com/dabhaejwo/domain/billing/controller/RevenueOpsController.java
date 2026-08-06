package com.dabhaejwo.domain.billing.controller;

import com.dabhaejwo.domain.billing.dto.response.BillingRecordResponse;
import com.dabhaejwo.domain.billing.dto.response.MonthlyRevenueResponse;
import com.dabhaejwo.domain.billing.dto.response.RevenueSummaryResponse;
import com.dabhaejwo.domain.billing.entity.BillingStatus;
import com.dabhaejwo.domain.billing.service.RevenueService;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 정산 조회 (api-contracts.md §6-1).
 *
 * <p><b>GET 만 둔다.</b> {@code billing_records} 는 원장이고 그것을 쓰는 곳은
 * {@code BillingService} 하나다. 여기에 쓰기를 두면 "이 금액이 왜 이런가"의 답이
 * 두 갈래가 된다 — 모델 단가 화면에 {@code PATCH} 를 두지 않은 것과 같은 이유다.
 */
@RestController
@RequestMapping("/api/ops/revenue")
public class RevenueOpsController {

    private final RevenueService revenueService;

    public RevenueOpsController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping("/summary")
    @RequirePermission(Permission.REVENUE_READ)
    public RevenueSummaryResponse summary() {
        return revenueService.summary();
    }

    @GetMapping("/monthly")
    @RequirePermission(Permission.REVENUE_READ)
    public List<MonthlyRevenueResponse> monthly(@RequestParam(required = false) Integer months) {
        return revenueService.monthly(months == null ? RevenueService.defaultMonths() : months);
    }

    /**
     * @param period {@code 2026-08} 형태. 생략하면 이번 달
     * @param status 생략하면 전체
     */
    @GetMapping("/records")
    @RequirePermission(Permission.REVENUE_READ)
    public PageResponse<BillingRecordResponse> records(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) BillingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return revenueService.records(period, status, page, size);
    }
}
