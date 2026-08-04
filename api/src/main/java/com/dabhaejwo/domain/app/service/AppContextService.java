package com.dabhaejwo.domain.app.service;

import com.dabhaejwo.domain.app.dto.response.AppContextResponse;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
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

/**
 * 업체 대시보드 부팅 컨텍스트.
 *
 * <p>대리 접속 세션은 {@code memberId} 자리에 세션 id 가 들어오므로 담당자 조회가 실패한다.
 * 그 경우 담당자 정보를 비워 내려보내고 배너로 대리 접속임을 알린다.
 */
@Service
public class AppContextService {

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final TenantDailyUsageRepository dailyUsageRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public AppContextService(TenantRepository tenantRepository,
                             TenantMemberRepository memberRepository,
                             PlanRepository planRepository,
                             TenantDailyUsageRepository dailyUsageRepository,
                             KnowledgeDocumentRepository documentRepository) {
        this.tenantRepository = tenantRepository;
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public AppContextResponse current() {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();

        Tenant tenant = tenantRepository.findById(user.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        MemberResponse member = memberRepository
                .findByIdAndTenantId(user.memberId(), user.tenantId())
                .map(MemberResponse::from)
                .orElse(null);

        return new AppContextResponse(
                member,
                new AppContextResponse.TenantContext(
                        tenant.getId(),
                        tenant.getName(),
                        tenant.getPrimaryDomain(),
                        tenant.getPublishableKey(),
                        tenant.getStatus(),
                        new AppContextResponse.PlanRef(plan.getId(), plan.getName(), plan.getMonthlyFee())),
                usage(user.tenantId(), plan),
                // TODO(T7): 대리 접속 세션 정보 연결. 지금은 배너를 띄우지 않는다.
                null);
    }

    private AppContextResponse.Usage usage(UUID tenantId, Plan plan) {
        LocalDate today = LocalDate.now();
        long convCount = dailyUsageRepository.sumConvCount(tenantId, today.withDayOfMonth(1), today);
        long docCount = documentRepository.countActive(tenantId);
        return new AppContextResponse.Usage(convCount, plan.getConvLimit(), docCount, plan.getDocLimit());
    }

    /** 마지막 접속 시각 갱신. 팀원 화면의 "마지막 접속"과 이탈 판정에 쓰인다. */
    @Transactional
    public void touchLastSeen() {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();
        if (user.impersonating()) {
            // 운영자의 대리 접속을 업체 담당자의 활동으로 기록하면 이탈 지표가 왜곡된다.
            return;
        }
        memberRepository.findByIdAndTenantId(user.memberId(), user.tenantId())
                .ifPresent(TenantMember::touchLastSeen);
    }
}
