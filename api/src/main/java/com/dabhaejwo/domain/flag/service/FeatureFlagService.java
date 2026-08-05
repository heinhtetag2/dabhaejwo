package com.dabhaejwo.domain.flag.service;

import com.dabhaejwo.domain.flag.dto.request.FeatureFlagUpdateRequest;
import com.dabhaejwo.domain.flag.dto.response.FeatureFlagResponse;
import com.dabhaejwo.domain.flag.entity.FeatureFlag;
import com.dabhaejwo.domain.flag.entity.FlagScope;
import com.dabhaejwo.domain.flag.repository.FeatureFlagRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final AuditLogService auditLogService;

    public FeatureFlagService(FeatureFlagRepository flagRepository,
                              TenantRepository tenantRepository,
                              PlanRepository planRepository,
                              AuditLogService auditLogService) {
        this.flagRepository = flagRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> list() {
        List<FeatureFlag> flags = flagRepository.findAllByOrderByNameAsc();

        List<UUID> tenantIds = flags.stream()
                .flatMap(flag -> flag.getTargetTenantIds().stream())
                .distinct()
                .toList();
        Map<UUID, String> tenantNames = tenantRepository.findAllById(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
        Map<UUID, String> planNames = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Plan::getName));

        return flags.stream().map(flag -> toResponse(flag, tenantNames, planNames)).toList();
    }

    @Transactional
    public FeatureFlagResponse update(String key, FeatureFlagUpdateRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        FeatureFlag flag = flagRepository.findById(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.FLAG_NOT_FOUND));

        validateTargets(request);
        flag.update(request.scope(), request.targetTenantIds(), request.targetPlanId(), request.enabled());

        auditLogService.record(operator.operatorId(), AuditAction.FLAG_WRITE, null, "",
                Map.of("key", key, "scope", request.scope().name(), "enabled", request.enabled()));

        return list().stream()
                .filter(response -> response.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FLAG_NOT_FOUND));
    }

    /**
     * 대상 없는 공개는 켜 봐야 아무에게도 안 열린다. 그런데 화면에는 "켜짐"으로 보인다 —
     * 그 상태를 만들지 않는다.
     */
    private void validateTargets(FeatureFlagUpdateRequest request) {
        if (request.scope() == FlagScope.TENANTS
                && (request.targetTenantIds() == null || request.targetTenantIds().isEmpty())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "지정 업체 공개는 업체를 하나 이상 골라야 합니다");
        }
        if (request.scope() == FlagScope.PLAN && request.targetPlanId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "요금제 공개는 요금제를 골라야 합니다");
        }
        if (request.targetPlanId() != null && !planRepository.existsById(request.targetPlanId())) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
    }

    private FeatureFlagResponse toResponse(FeatureFlag flag,
                                           Map<UUID, String> tenantNames,
                                           Map<UUID, String> planNames) {
        List<String> names = new ArrayList<>();
        flag.getTargetTenantIds().forEach(id -> names.add(tenantNames.getOrDefault(id, "(삭제된 업체)")));
        return FeatureFlagResponse.of(flag, names,
                flag.getTargetPlanId() == null ? null : planNames.get(flag.getTargetPlanId()));
    }
}
