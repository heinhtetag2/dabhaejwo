package com.dabhaejwo.domain.plan.controller;

import com.dabhaejwo.domain.plan.dto.request.PlanCreateRequest;
import com.dabhaejwo.domain.plan.dto.request.PlanModelAssignmentRequest;
import com.dabhaejwo.domain.plan.dto.request.PlanUpdateRequest;
import com.dabhaejwo.domain.plan.dto.response.PlanModelAssignmentResponse;
import com.dabhaejwo.domain.plan.dto.response.PlanResponse;
import com.dabhaejwo.domain.plan.service.PlanOpsService;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 요금제 정의. <b>DELETE 가 없다.</b> 판매 중단({@code sellable: false})만 한다 —
 * 기존 계약 업체가 남아 있다.
 */
@RestController
@RequestMapping("/api/ops")
public class PlanOpsController {

    private final PlanOpsService service;

    public PlanOpsController(PlanOpsService service) {
        this.service = service;
    }

    @GetMapping("/plans")
    @RequirePermission(Permission.PLAN_READ)
    public List<PlanResponse> list() {
        return service.list();
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.PLAN_WRITE)
    public PlanResponse create(@Valid @RequestBody PlanCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/plans/{planId}")
    @RequirePermission(Permission.PLAN_WRITE)
    public PlanResponse update(@PathVariable UUID planId,
                               @Valid @RequestBody PlanUpdateRequest request) {
        return service.update(planId, request);
    }

    @GetMapping("/plan-model-assignments")
    @RequirePermission(Permission.PLAN_READ)
    public List<PlanModelAssignmentResponse> assignments() {
        return service.assignments();
    }

    /** 모델 배정 변경은 전체 원가에 직결되므로 {@code MODEL_PRICE_WRITE}(OPS_ADMIN 전용)를 요구한다. */
    @PutMapping("/plan-model-assignments")
    @RequirePermission(Permission.MODEL_PRICE_WRITE)
    public PlanModelAssignmentResponse saveAssignment(
            @Valid @RequestBody PlanModelAssignmentRequest request) {
        return service.saveAssignment(request);
    }
}
