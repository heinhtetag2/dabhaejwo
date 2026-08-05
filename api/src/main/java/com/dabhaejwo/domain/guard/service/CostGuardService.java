package com.dabhaejwo.domain.guard.service;

import com.dabhaejwo.domain.guard.dto.request.CostGuardUpdateRequest;
import com.dabhaejwo.domain.guard.dto.response.CostGuardResponse;
import com.dabhaejwo.domain.guard.entity.CostGuard;
import com.dabhaejwo.domain.guard.repository.CostGuardRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CostGuardService {

    private final CostGuardRepository repository;
    private final AuditLogService auditLogService;

    public CostGuardService(CostGuardRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public CostGuardResponse current() {
        return CostGuardResponse.from(repository.current());
    }

    @Transactional
    public CostGuardResponse update(CostGuardUpdateRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        CostGuard guard = repository.current();

        if (request.tenantDailyCapKrw() > request.globalDailyCapKrw()) {
            // 업체 상한이 전체 상한보다 크면 전체 상한이 먼저 걸려 업체 상한이 무의미해진다.
            // 화면에는 둘 다 설정된 것처럼 보이므로 저장 시점에 막는다.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "업체별 상한은 전체 상한보다 클 수 없습니다");
        }

        guard.update(
                request.tenantDailyCapKrw(),
                request.globalDailyCapKrw(),
                request.ipQuestionsPerMin(),
                request.bulkUploadLimit(),
                request.costRatioWarnPercent(),
                request.answerFailSimilarity(),
                request.defaultChunkCount(),
                request.answerMaxLength(),
                request.churnPurgeGraceDays(),
                request.quotaExceededBehavior(),
                request.slackAlertEnabled(),
                request.commonPrompt());

        auditLogService.record(operator.operatorId(), AuditAction.COST_GUARD_WRITE, null,
                request.reason(),
                Map.of("tenantDailyCapKrw", request.tenantDailyCapKrw(),
                        "globalDailyCapKrw", request.globalDailyCapKrw(),
                        "costRatioWarnPercent", request.costRatioWarnPercent()));

        return CostGuardResponse.from(guard);
    }

    /**
     * 임베딩 공급사 교체.
     *
     * <p>안전장치 저장과 <b>경로를 나눈 것이 의도다.</b> 이 값을 바꾸면 이미 학습된 조각이
     * 전부 무효가 된다 — 다른 모델이 만든 벡터끼리는 거리를 비교할 수 없다.
     * 업체가 "다시 학습"을 눌러야 새 공급사로 다시 만들어진다.
     */
    @Transactional
    public CostGuardResponse changeEmbeddingProvider(String provider, String reason) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        CostGuard guard = repository.current();

        String before = guard.getEmbeddingProvider();
        guard.changeEmbeddingProvider(provider);

        auditLogService.record(operator.operatorId(), AuditAction.COST_GUARD_WRITE, null, reason,
                Map.of("setting", "embeddingProvider", "from", before, "to", provider));

        return CostGuardResponse.from(guard);
    }
}
