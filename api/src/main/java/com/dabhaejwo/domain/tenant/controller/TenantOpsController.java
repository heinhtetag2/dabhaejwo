package com.dabhaejwo.domain.tenant.controller;

import com.dabhaejwo.domain.tenant.dto.response.TenantSummaryResponse;
import com.dabhaejwo.domain.tenant.service.TenantQueryService;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영 콘솔의 업체 API. 요청 검증 + 서비스 호출 + 응답 변환만 한다.
 */
@RestController
@RequestMapping("/api/ops/tenants")
public class TenantOpsController {

    private final TenantQueryService queryService;

    public TenantOpsController(TenantQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @RequirePermission(Permission.TENANT_READ)
    public PageResponse<TenantSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "COST_RATIO_DESC") TenantQueryService.TenantSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return queryService.list(q, sort, page, size);
    }
}
