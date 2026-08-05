package com.dabhaejwo.domain.tenant.service;

import com.dabhaejwo.domain.guard.repository.CostGuardRepository;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationSession;
import com.dabhaejwo.domain.impersonation.entity.ImpersonationStatus;
import com.dabhaejwo.domain.impersonation.repository.ImpersonationSessionRepository;
import com.dabhaejwo.domain.operator.service.OperatorLookupService;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.dto.request.QuotaOverrideRequest;
import com.dabhaejwo.domain.tenant.dto.request.TenantNoteRequest;
import com.dabhaejwo.domain.tenant.dto.request.TenantPlanChangeRequest;
import com.dabhaejwo.domain.tenant.dto.request.TenantStatusChangeRequest;
import com.dabhaejwo.domain.tenant.dto.request.TrialExtensionRequest;
import com.dabhaejwo.domain.tenant.dto.response.QuotaOverrideResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantDetailResponse;
import com.dabhaejwo.domain.tenant.dto.response.TenantNoteResponse;
import com.dabhaejwo.domain.tenant.entity.QuotaOverride;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantNote;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.QuotaOverrideRepository;
import com.dabhaejwo.domain.tenant.repository.TenantNoteRepository;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 업체에 대한 운영자 조치.
 *
 * <p>모든 쓰기 액션은 <b>같은 트랜잭션 안에서</b> 감사 기록을 남긴다. 사유가 필요한 행위는
 * {@link AuditLogService} 가 사유 없으면 거부하므로, 사유 검증이 화면과 서비스 두 곳에
 * 흩어지지 않는다 (core security-rules).
 */
@Service
public class TenantAdminService {

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final QuotaOverrideRepository quotaOverrideRepository;
    private final TenantNoteRepository noteRepository;
    private final ImpersonationSessionRepository sessionRepository;
    private final CostGuardRepository costGuardRepository;
    private final TenantQueryService queryService;
    private final OperatorLookupService operatorLookup;
    private final AuditLogService auditLogService;

    public TenantAdminService(TenantRepository tenantRepository,
                              PlanRepository planRepository,
                              QuotaOverrideRepository quotaOverrideRepository,
                              TenantNoteRepository noteRepository,
                              ImpersonationSessionRepository sessionRepository,
                              CostGuardRepository costGuardRepository,
                              TenantQueryService queryService,
                              OperatorLookupService operatorLookup,
                              AuditLogService auditLogService) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.quotaOverrideRepository = quotaOverrideRepository;
        this.noteRepository = noteRepository;
        this.sessionRepository = sessionRepository;
        this.costGuardRepository = costGuardRepository;
        this.queryService = queryService;
        this.operatorLookup = operatorLookup;
        this.auditLogService = auditLogService;
    }

    /**
     * 상태 변경. 허용 전이는 {@code TenantStatus} 가 갖고 있고 무효 전이는
     * {@code INVALID_STATE_TRANSITION} 이다 — "일단 값만 바꾸기"를 하지 않는다.
     */
    @Transactional
    public TenantDetailResponse changeStatus(UUID tenantId, TenantStatusChangeRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Tenant tenant = load(tenantId);

        // 전이를 먼저 검증·적용하고 그 뒤에 기록한다. 순서를 뒤집으면 무효 전이로 실패한 요청까지
        // "정지했다"로 남는다 — 감사 기록은 REQUIRES_NEW 라 바깥 롤백에 따라오지 않는다.
        // 반대로 기록이 실패하면(사유 누락) 바깥 트랜잭션이 통째로 롤백되므로 상태만 바뀌는 일은 없다.
        switch (request.status()) {
            case SUSPENDED -> {
                tenant.suspend();
                auditLogService.record(operator.operatorId(), AuditAction.SUSPEND, tenantId, request.reason());
            }
            case CHURNED -> {
                tenant.churn(costGuardRepository.current().getChurnPurgeGraceDays());
                revokeImpersonations(tenantId);
                auditLogService.record(operator.operatorId(), AuditAction.CHURN, tenantId, request.reason());
            }
            case ACTIVE -> {
                tenant.activate();
                auditLogService.record(operator.operatorId(), AuditAction.ACTIVATE, tenantId, request.reason());
            }
            // TRIAL 로 되돌리는 경로는 두지 않는다. 체험은 가입으로만 시작하고,
            // 되돌리면 "체험을 두 번 쓴" 계정이 생긴다.
            case TRIAL -> throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "체험 상태로 되돌릴 수 없습니다");
        }

        return queryService.detail(tenantId);
    }

    /**
     * 대상 업체가 해지되면 진행 중인 대리 접속 세션을 즉시 끊는다.
     * 해지된 업체의 고객 데이터를 계속 보고 있을 이유가 없다.
     */
    private void revokeImpersonations(UUID tenantId) {
        List<ImpersonationSession> active =
                sessionRepository.findAllByTenantIdAndStatus(tenantId, ImpersonationStatus.ACTIVE);
        active.forEach(ImpersonationSession::revoke);
    }

    @Transactional
    public TenantDetailResponse changePlan(UUID tenantId, TenantPlanChangeRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Tenant tenant = load(tenantId);
        Plan target = planRepository.findById(request.planId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
        Plan before = planRepository.findById(tenant.getPlanId()).orElse(null);

        tenant.changePlan(target.getId());
        auditLogService.record(operator.operatorId(), AuditAction.CHANGE_PLAN, tenantId, request.reason(),
                Map.of("from", before == null ? "(없음)" : before.getName(),
                        "to", target.getName()));

        return queryService.detail(tenantId);
    }

    @Transactional
    public TenantDetailResponse extendTrial(UUID tenantId, TrialExtensionRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        Tenant tenant = load(tenantId);

        tenant.extendTrial(request.days());
        auditLogService.record(operator.operatorId(), AuditAction.EXTEND_TRIAL, tenantId, request.reason(),
                Map.of("days", request.days()));

        return queryService.detail(tenantId);
    }

    /**
     * 이번 달 한정 쿼터 증량. 요금제 한도를 고치지 않고 델타만 쌓으므로 다음 달 자동 원복된다.
     */
    @Transactional
    public QuotaOverrideResponse grantQuota(UUID tenantId, QuotaOverrideRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        load(tenantId);

        if (request.convDelta() == 0 && request.docDelta() == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "증량할 대화 수나 문서 수 중 하나는 0이 아니어야 합니다");
        }

        auditLogService.record(operator.operatorId(), AuditAction.GRANT_QUOTA, tenantId, request.reason(),
                Map.of("convDelta", request.convDelta(), "docDelta", request.docDelta()));

        QuotaOverride saved = quotaOverrideRepository.save(QuotaOverride.grant(
                tenantId, LocalDate.now(), request.convDelta(), request.docDelta(),
                request.reason().strip(), operator.operatorId()));

        return QuotaOverrideResponse.of(saved,
                new QuotaOverrideResponse.OperatorRef(operator.operatorId(), operator.name()));
    }

    @Transactional(readOnly = true)
    public List<QuotaOverrideResponse> listQuotaOverrides(UUID tenantId) {
        List<QuotaOverride> overrides = quotaOverrideRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        Map<UUID, String> names = operatorLookup.namesOf(
                overrides.stream().map(QuotaOverride::getOperatorId).toList());
        return overrides.stream()
                .map(override -> QuotaOverrideResponse.of(override,
                        new QuotaOverrideResponse.OperatorRef(
                                override.getOperatorId(),
                                operatorLookup.nameOf(names, override.getOperatorId()))))
                .toList();
    }

    /**
     * 내부 메모는 누적이다. 수정·삭제 메서드를 두지 않는다 —
     * 영업 이력과 CS 맥락이 담기므로 덮어쓰면 "왜 이렇게 대응했는지"가 사라진다.
     *
     * <p>감사 기록을 따로 남기지 않는 이유는 메모 자체가 작성자와 시각을 갖고 있어
     * 같은 사실을 두 번 쓰는 셈이 되기 때문이다.
     */
    @Transactional
    public TenantNoteResponse addNote(UUID tenantId, TenantNoteRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        load(tenantId);

        TenantNote saved = noteRepository.save(
                TenantNote.write(tenantId, request.body().strip(), operator.operatorId()));

        return TenantNoteResponse.of(saved,
                new TenantNoteResponse.OperatorRef(operator.operatorId(), operator.name()));
    }

    @Transactional(readOnly = true)
    public List<TenantNoteResponse> listNotes(UUID tenantId) {
        List<TenantNote> notes = noteRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        Map<UUID, String> names = operatorLookup.namesOf(
                notes.stream().map(TenantNote::getOperatorId).toList());
        return notes.stream()
                .map(note -> TenantNoteResponse.of(note,
                        new TenantNoteResponse.OperatorRef(
                                note.getOperatorId(),
                                operatorLookup.nameOf(names, note.getOperatorId()))))
                .toList();
    }

    private Tenant load(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        if (tenant.getStatus() == TenantStatus.CHURNED) {
            // 해지된 업체는 읽기 전용이다 (admin-console-tenant-plan.md §9).
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "해지된 업체에는 조치할 수 없습니다");
        }
        return tenant;
    }
}
