package com.dabhaejwo.domain.today.service;

import com.dabhaejwo.domain.billing.service.TicketOpsService;
import com.dabhaejwo.domain.job.dto.response.JobStatsResponse;
import com.dabhaejwo.domain.job.entity.JobStatus;
import com.dabhaejwo.domain.job.service.JobOpsService;
import com.dabhaejwo.domain.tenant.dto.response.TenantSummaryResponse;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.service.TenantQueryService;
import com.dabhaejwo.domain.today.dto.response.TodaySummaryResponse;
import com.dabhaejwo.domain.usage.entity.CostRatio;
import com.dabhaejwo.domain.usage.service.DailyUsageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 로그인 직후 오늘 해야 할 일.
 *
 * <p>화면 최상단에는 <b>문장 하나</b>만 크게 둔다. 지표 카드를 나열하면 무엇이 중요한지
 * 사라진다 (admin-console-plan.md §2.2).
 *
 * <p>시스템 상태의 미측정 값은 {@code null} 로 내려보낸다. 0 으로 채우면 "응답 0ms,
 * 오류 0건"이라는 거짓이 되고 운영자는 정상이라고 읽는다.
 */
@Service
public class TodayService {

    /** 조치 목록에 올릴 원가 초과 업체 상한. 전부 나열하면 목록이 아니라 벽이 된다. */
    private static final int MAX_COST_EXCEEDED_ROWS = 5;
    /** 체험 종료 임박 기준 (admin-console-plan.md §4.1). */
    private static final int TRIAL_ENDING_DAYS = 3;
    private static final int MAX_RECENT_ERRORS = 5;

    private final TenantQueryService tenantQueryService;
    private final JobOpsService jobOpsService;
    private final TicketOpsService ticketOpsService;
    private final DailyUsageService dailyUsageService;

    public TodayService(TenantQueryService tenantQueryService,
                        JobOpsService jobOpsService,
                        TicketOpsService ticketOpsService,
                        DailyUsageService dailyUsageService) {
        this.tenantQueryService = tenantQueryService;
        this.jobOpsService = jobOpsService;
        this.ticketOpsService = ticketOpsService;
        this.dailyUsageService = dailyUsageService;
    }

    @Transactional(readOnly = true)
    public TodaySummaryResponse summary() {
        List<TenantSummaryResponse> costExceeded =
                tenantQueryService.costExceeded(CostRatio.LOSS_THRESHOLD_PERCENT);
        TenantQueryService.Revenue revenue = tenantQueryService.revenue();
        DailyUsageService.Total today = dailyUsageService.today();
        JobStatsResponse jobStats = jobOpsService.stats();

        return new TodaySummaryResponse(
                new TodaySummaryResponse.Headline(
                        tenantQueryService.activeTenantCount(), costExceeded.size()),
                new TodaySummaryResponse.Stats(
                        revenue.payingTenantCount(),
                        revenue.mrrKrw(),
                        today.convCount(),
                        today.costKrw()),
                actions(costExceeded, jobStats),
                system(jobStats),
                dailyUsageService.lastAggregatedAt());
    }

    private List<TodaySummaryResponse.Action> actions(List<TenantSummaryResponse> costExceeded,
                                                      JobStatsResponse jobStats) {
        List<TodaySummaryResponse.Action> actions = new ArrayList<>();

        costExceeded.stream().limit(MAX_COST_EXCEEDED_ROWS).forEach(tenant -> actions.add(
                new TodaySummaryResponse.Action(
                        TodaySummaryResponse.Action.Type.COST_EXCEEDED,
                        tenant.id(),
                        tenant.name(),
                        planLabel(tenant) + " / 원가 " + tenant.costKrw().toBigInteger() + "원",
                        // 원가는 수익성 화면에서 분해해 본다.
                        "/profitability")));

        if (jobStats.failedCount() > 0) {
            actions.add(new TodaySummaryResponse.Action(
                    TodaySummaryResponse.Action.Type.JOB_FAILED,
                    null,
                    "작업 실패 " + jobStats.failedCount() + "건",
                    "재시도를 모두 소진한 작업이 있습니다",
                    "/jobs"));
        }

        tenantQueryService.paymentFailed().forEach(tenant -> actions.add(
                new TodaySummaryResponse.Action(
                        TodaySummaryResponse.Action.Type.PAYMENT_FAILED,
                        tenant.id(),
                        tenant.name(),
                        "최근 결제 시도가 실패했습니다",
                        "/tenants?tenantId=" + tenant.id())));

        List<Tenant> trialEnding = tenantQueryService.trialEndingSoon(TRIAL_ENDING_DAYS);
        if (!trialEnding.isEmpty()) {
            Tenant first = trialEnding.get(0);
            actions.add(new TodaySummaryResponse.Action(
                    TodaySummaryResponse.Action.Type.TRIAL_ENDING,
                    first.getId(),
                    trialEnding.size() == 1
                            ? first.getName()
                            : first.getName() + " 외 " + (trialEnding.size() - 1) + "곳",
                    TRIAL_ENDING_DAYS + "일 내 무료 체험 종료",
                    "/tenants?filter=TRIAL"));
        }

        long openTickets = ticketOpsService.openCount();
        if (openTickets > 0) {
            actions.add(new TodaySummaryResponse.Action(
                    TodaySummaryResponse.Action.Type.TICKET_WAITING,
                    null,
                    "답변 없는 문의 " + openTickets + "건",
                    "오래된 것이 위로 옵니다",
                    "/tickets"));
        }

        return actions;
    }

    private String planLabel(TenantSummaryResponse tenant) {
        if (tenant.plan() == null) {
            return "요금제 없음";
        }
        return tenant.plan().name() + " " + tenant.billedKrw() + "원";
    }

    /**
     * 시스템 상태.
     *
     * <p>{@code embedQueueDepth} 만 실값이다 — {@code jobs} 에서 실제로 센다.
     * 나머지는 <b>측정 지점 자체가 없다.</b> 답변 파이프라인이 붙으면 응답 시간이,
     * 워커가 붙으면 가동 대수가, APM 이 붙으면 5xx 가 채워진다. 그때까지 null 이다.
     */
    private TodaySummaryResponse.System system(JobStatsResponse jobStats) {
        List<TodaySummaryResponse.System.RecentError> recentErrors =
                jobOpsService.list(JobStatus.FAILED, 0, MAX_RECENT_ERRORS).content().stream()
                        .map(job -> new TodaySummaryResponse.System.RecentError(
                                job.updatedAt(),
                                job.tenant() == null ? null : job.tenant().name(),
                                job.errorCode()))
                        .toList();

        return new TodaySummaryResponse.System(
                null,
                jobStats.queuedCount(),
                null,
                null,
                null,
                recentErrors);
    }
}
