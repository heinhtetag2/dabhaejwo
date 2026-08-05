package com.dabhaejwo.domain.plan.service;

import com.dabhaejwo.domain.plan.dto.request.PlanCreateRequest;
import com.dabhaejwo.domain.plan.dto.request.PlanModelAssignmentRequest;
import com.dabhaejwo.domain.plan.dto.request.PlanUpdateRequest;
import com.dabhaejwo.domain.plan.dto.response.PlanModelAssignmentResponse;
import com.dabhaejwo.domain.plan.dto.response.PlanResponse;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.entity.PlanModelAssignment;
import com.dabhaejwo.domain.plan.repository.PlanModelAssignmentRepository;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.pricing.service.ModelPriceService;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 요금제 정의와 요금제별 모델 배정.
 *
 * <p><b>삭제가 없다.</b> 판매 중단({@code sellable = false})만 한다 —
 * 기존 계약 업체가 남아 있고, 그 업체의 요금제가 사라지면 청구액을 계산할 수 없다.
 */
@Service
public class PlanOpsService {

    private final PlanRepository planRepository;
    private final PlanModelAssignmentRepository assignmentRepository;
    private final TenantRepository tenantRepository;
    private final ModelPriceService modelPriceService;
    private final AuditLogService auditLogService;

    public PlanOpsService(PlanRepository planRepository,
                          PlanModelAssignmentRepository assignmentRepository,
                          TenantRepository tenantRepository,
                          ModelPriceService modelPriceService,
                          AuditLogService auditLogService) {
        this.planRepository = planRepository;
        this.assignmentRepository = assignmentRepository;
        this.tenantRepository = tenantRepository;
        this.modelPriceService = modelPriceService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        Map<UUID, Long> usage = tenantRepository.countByPlan().stream()
                .collect(Collectors.toMap(TenantRepository.PlanUsage::getPlanId,
                        TenantRepository.PlanUsage::getCount));
        // 판매 중단된 구 요금제도 목록에 남긴다 — 사용 업체 수를 보여줘야 하기 때문이다.
        return planRepository.findAllByOrderBySortOrderAsc().stream()
                .map(plan -> PlanResponse.of(plan, usage.getOrDefault(plan.getId(), 0L)))
                .toList();
    }

    @Transactional
    public PlanResponse create(PlanCreateRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        planRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "이미 있는 요금제 코드입니다: " + request.code());
        });

        Plan saved = planRepository.save(Plan.create(
                request.code(), request.name(), request.monthlyFee(), request.negotiable(),
                request.convLimit(), request.docLimit(), request.sortOrder()));

        auditLogService.record(operator.operatorId(), AuditAction.PLAN_WRITE, null, "",
                Map.of("action", "create", "code", saved.getCode()));

        return PlanResponse.of(saved, 0L);
    }

    @Transactional
    public PlanResponse update(UUID planId, PlanUpdateRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        plan.update(request.name(), request.monthlyFee(), request.negotiable(),
                request.convLimit(), request.docLimit(), request.sellable());

        auditLogService.record(operator.operatorId(), AuditAction.PLAN_WRITE, null, "",
                Map.of("action", "update", "code", plan.getCode(), "sellable", request.sellable()));

        long tenantCount = tenantRepository.countByPlan().stream()
                .filter(row -> planId.equals(row.getPlanId()))
                .map(TenantRepository.PlanUsage::getCount)
                .findFirst()
                .orElse(0L);

        return PlanResponse.of(plan, tenantCount);
    }

    @Transactional(readOnly = true)
    public List<PlanModelAssignmentResponse> assignments() {
        Map<UUID, PlanModelAssignment> byPlan = assignmentRepository.findAll().stream()
                .collect(Collectors.toMap(PlanModelAssignment::getPlanId, assignment -> assignment));

        return planRepository.findAllByOrderBySortOrderAsc().stream()
                .map(plan -> toResponse(plan, byPlan.get(plan.getId())))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public PlanModelAssignmentResponse saveAssignment(PlanModelAssignmentRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        // 단가가 없는 모델을 배정하면 그 요금제의 모든 답변이 원가 계산에서 터진다.
        // 배정 시점에 막는다 — 실제 대화가 실패하고 나서 알게 되면 늦다.
        modelPriceService.resolve(request.provider(), request.model(), java.time.OffsetDateTime.now());

        PlanModelAssignment assignment = assignmentRepository.findById(request.planId())
                .map(existing -> {
                    existing.update(request.provider(), request.model(), request.chunkCount());
                    return existing;
                })
                .orElseGet(() -> assignmentRepository.save(PlanModelAssignment.of(
                        request.planId(), request.provider(), request.model(), request.chunkCount())));

        auditLogService.record(operator.operatorId(), AuditAction.MODEL_PRICE_WRITE, null,
                "요금제 모델 배정 변경",
                Map.of("plan", plan.getCode(), "model", request.model(),
                        "chunkCount", request.chunkCount()));

        return toResponse(plan, assignment);
    }

    private PlanModelAssignmentResponse toResponse(Plan plan, PlanModelAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new PlanModelAssignmentResponse(
                new PlanModelAssignmentResponse.PlanRef(plan.getId(), plan.getName()),
                assignment.getProvider(),
                assignment.getModel(),
                assignment.getChunkCount(),
                modelPriceService.estimateCostPerConversation(
                        assignment.getProvider(), assignment.getModel(), assignment.getChunkCount()));
    }
}
