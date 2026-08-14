package com.dabhaejwo.domain.billing.service;

import com.dabhaejwo.domain.billing.dto.request.UpgradeRequestBody;
import com.dabhaejwo.domain.billing.dto.response.PlanOverviewResponse;
import com.dabhaejwo.domain.billing.entity.Ticket;
import com.dabhaejwo.domain.billing.entity.TicketStatus;
import com.dabhaejwo.domain.billing.repository.BillingRecordRepository;
import com.dabhaejwo.domain.billing.repository.TicketRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.notification.service.NotificationEvents;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.domain.usage.repository.TenantDailyUsageRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
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
    private final TicketRepository ticketRepository;
    private final NotificationEvents notificationEvents;

    public PlanOverviewService(TenantRepository tenantRepository,
                               PlanRepository planRepository,
                               TenantDailyUsageRepository dailyUsageRepository,
                               KnowledgeDocumentRepository documentRepository,
                               BillingRecordRepository billingRepository,
                               TicketRepository ticketRepository,
                               NotificationEvents notificationEvents) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.documentRepository = documentRepository;
        this.billingRepository = billingRepository;
        this.ticketRepository = ticketRepository;
        this.notificationEvents = notificationEvents;
    }

    /**
     * 유료 전환 신청. PG 가 붙기 전까지 문의로 접수하고 운영팀이 수동 처리한다.
     *
     * <p>결제가 일어난 것처럼 {@code billing_records} 를 만들지 않는다 —
     * 받지 않은 돈을 받은 것으로 적으면 정산이 틀어진다 (tenant-public-plan.md §5.2).
     */
    @Transactional
    public void requestUpgrade(UpgradeRequestBody request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        Plan target = planRepository.findByCode(request.planCode().strip())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
        if (!target.isSellable()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "판매 중인 요금제가 아닙니다");
        }

        String subject = "유료 전환 신청 — " + target.getName();
        if (ticketRepository.existsByTenantIdAndSubjectAndStatus(
                user.tenantId(), subject, TicketStatus.OPEN)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "이미 접수된 신청이 있습니다. 곧 담당자가 연락드립니다");
        }

        String note = (request.note() == null || request.note().isBlank())
                ? "(추가 요청 없음)" : request.note().strip();
        Ticket ticket = ticketRepository.save(Ticket.open(user.tenantId(), subject,
                "요금제 코드: " + target.getCode() + "\n요청: " + note));

        // PG 가 붙기 전까지 "돈 내겠다"는 신호가 이 티켓 하나뿐이다. 놓치면 매출을 놓친다.
        notificationEvents.ticketOpened(user.tenantId(), tenantName(user.tenantId()),
                ticket.getId(), subject);
    }

    private String tenantName(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(Tenant::getName).orElse(tenantId.toString());
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
        long docCount = documentRepository.countActiveAcrossBots(tenantId);

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
