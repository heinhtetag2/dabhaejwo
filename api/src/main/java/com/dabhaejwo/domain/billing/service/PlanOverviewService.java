package com.dabhaejwo.domain.billing.service;

import com.dabhaejwo.domain.billing.dto.response.PlanOverviewResponse;
import com.dabhaejwo.domain.billing.repository.BillingRecordRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/** 요금제·사용량·결제 내역. */
@Service
public class PlanOverviewService {

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final TenantDailyUsageRepository dailyUsageRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final BillingRecordRepository billingRepository;

    public PlanOverviewService(TenantRepository tenantRepository,
                               PlanRepository planRepository,
                               TenantDailyUsageRepository dailyUsageRepository,
                               KnowledgeDocumentRepository documentRepository,
                               BillingRecordRepository billingRepository) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.documentRepository = documentRepository;
        this.billingRepository = billingRepository;
    }

    @Transactional(readOnly = true)
    public PlanOverviewResponse overview() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate from = today.withDayOfMonth(1);
        long convCount = dailyUsageRepository.sumConvCount(tenantId, from, today);
        long docCount = documentRepository.countActive(tenantId);

        return new PlanOverviewResponse(
                new PlanOverviewResponse.Plan(plan.getId(), plan.getName(), plan.getMonthlyFee()),
                new PlanOverviewResponse.Usage(convCount, plan.getConvLimit(), docCount, plan.getDocLimit()),
                tenant.getNextBillingDate(),
                savedAnswerPercent(tenantId, from, today),
                PlanOverviewResponse.toItems(billingRepository.findAllByTenantIdOrderByPeriodDesc(tenantId)));
    }

    /**
     * 저장 답변 비율. 대화가 한 건도 없으면 null 이다 —
     * 0% 로 내리면 "공통 질문이 하나도 안 쓰였다"로 읽히는데 사실은 대화 자체가 없었던 것이다.
     */
    private Integer savedAnswerPercent(UUID tenantId, LocalDate from, LocalDate to) {
        var totals = dailyUsageRepository.aggregateBetween(from, to).stream()
                .filter(row -> row.getTenantId().equals(tenantId))
                .findFirst();
        if (totals.isEmpty() || totals.get().getConvCount() == 0) {
            return null;
        }
        return Math.toIntExact(Math.round(
                totals.get().getSavedCount() * 100.0 / totals.get().getConvCount()));
    }
}
