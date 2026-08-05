package com.dabhaejwo.domain.tenant.controller;

import com.dabhaejwo.domain.tenant.dto.request.QuotaOverrideRequest;
import com.dabhaejwo.domain.tenant.dto.request.TenantNoteRequest;
import com.dabhaejwo.domain.tenant.dto.request.TenantPlanChangeRequest;
import com.dabhaejwo.domain.tenant.dto.request.TenantStatusChangeRequest;
import com.dabhaejwo.domain.tenant.dto.request.TrialExtensionRequest;
import com.dabhaejwo.domain.tenant.dto.response.QuotaOverrideResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantActivityResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantDetailResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantFilterCountsResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantNoteResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantSummaryResponse;
import com.dabhaejwo.domain.tenant.service.TenantActivityService;
import com.dabhaejwo.domain.tenant.service.TenantAdminService;
import com.dabhaejwo.domain.tenant.service.TenantQueryService;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 운영 콘솔의 업체 API. 요청 검증 + 서비스 호출 + 응답 변환만 한다.
 *
 * <p>권한은 {@link RequirePermission} 이 선언적으로 건다 — 핸들러마다 if-role 분기를
 * 흩뿌리지 않는다. 역할별 허용 범위는 admin-console-plan.md §7 매트릭스와 일치한다.
 */
@RestController
@RequestMapping("/api/ops/tenants")
public class TenantOpsController {

    private final TenantQueryService queryService;
    private final TenantAdminService adminService;
    private final TenantActivityService activityService;

    public TenantOpsController(TenantQueryService queryService,
                               TenantAdminService adminService,
                               TenantActivityService activityService) {
        this.queryService = queryService;
        this.adminService = adminService;
        this.activityService = activityService;
    }

    @GetMapping
    @RequirePermission(Permission.TENANT_READ)
    public PageResponse<TenantSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "ALL") TenantQueryService.TenantFilter filter,
            @RequestParam(defaultValue = "COST_RATIO_DESC") TenantQueryService.TenantSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return queryService.list(q, filter, sort, page, size);
    }

    /** 필터 칩별 건수. 0인 칩도 내려간다 — 화면은 흐리게 처리하되 숨기지 않는다. */
    @GetMapping("/filters")
    @RequirePermission(Permission.TENANT_READ)
    public TenantFilterCountsResponse filters() {
        return queryService.filterCounts();
    }

    @GetMapping("/{tenantId}")
    @RequirePermission(Permission.TENANT_READ)
    public TenantDetailResponse detail(@PathVariable UUID tenantId) {
        return queryService.detail(tenantId);
    }

    @GetMapping("/{tenantId}/activities")
    @RequirePermission(Permission.TENANT_READ)
    public PageResponse<TenantActivityResponse> activities(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return activityService.list(tenantId, page, size);
    }

    @PatchMapping("/{tenantId}/status")
    @RequirePermission(Permission.TENANT_STATUS_WRITE)
    public TenantDetailResponse changeStatus(@PathVariable UUID tenantId,
                                             @Valid @RequestBody TenantStatusChangeRequest request) {
        return adminService.changeStatus(tenantId, request);
    }

    @PatchMapping("/{tenantId}/plan")
    @RequirePermission(Permission.TENANT_PLAN_WRITE)
    public TenantDetailResponse changePlan(@PathVariable UUID tenantId,
                                           @Valid @RequestBody TenantPlanChangeRequest request) {
        return adminService.changePlan(tenantId, request);
    }

    @PostMapping("/{tenantId}/trial-extension")
    @RequirePermission(Permission.TENANT_TRIAL_WRITE)
    public TenantDetailResponse extendTrial(@PathVariable UUID tenantId,
                                            @Valid @RequestBody TrialExtensionRequest request) {
        return adminService.extendTrial(tenantId, request);
    }

    @GetMapping("/{tenantId}/quota-overrides")
    @RequirePermission(Permission.TENANT_READ)
    public List<QuotaOverrideResponse> quotaOverrides(@PathVariable UUID tenantId) {
        return adminService.listQuotaOverrides(tenantId);
    }

    @PostMapping("/{tenantId}/quota-overrides")
    @RequirePermission(Permission.QUOTA_GRANT)
    public QuotaOverrideResponse grantQuota(@PathVariable UUID tenantId,
                                            @Valid @RequestBody QuotaOverrideRequest request) {
        return adminService.grantQuota(tenantId, request);
    }

    @GetMapping("/{tenantId}/notes")
    @RequirePermission(Permission.TENANT_READ)
    public List<TenantNoteResponse> notes(@PathVariable UUID tenantId) {
        return adminService.listNotes(tenantId);
    }

    /** 메모는 누적이다. 수정·삭제 엔드포인트가 없는 것이 설계다. */
    @PostMapping("/{tenantId}/notes")
    @RequirePermission(Permission.TENANT_NOTE_WRITE)
    public TenantNoteResponse addNote(@PathVariable UUID tenantId,
                                      @Valid @RequestBody TenantNoteRequest request) {
        return adminService.addNote(tenantId, request);
    }
}
