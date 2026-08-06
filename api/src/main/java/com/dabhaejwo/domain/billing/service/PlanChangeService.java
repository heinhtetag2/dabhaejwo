package com.dabhaejwo.domain.billing.service;

import com.dabhaejwo.domain.billing.dto.response.PlanChangeResponse;
import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.entity.BillingStatus;
import com.dabhaejwo.domain.billing.repository.TenantBillingRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.common.BusinessDay;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업체가 스스로 요금제를 바꾼다.
 *
 * <p>예전에는 신청서를 받아 운영자가 수동으로 처리했다. 이제 <b>고른 즉시 결제된다.</b>
 *
 * <p>언제 돈이 나가는지가 이 클래스의 전부다:
 * <ul>
 *   <li><b>무료·체험 → 유료</b>: 지금 결제한다. 결제가 성공해야 요금제가 바뀐다 —
 *       실패했는데 요금제만 올라가면 돈은 안 받고 한도만 열어준 셈이 된다
 *   <li><b>유료 → 유료</b>: <b>추가로 청구하지 않는다.</b> 이번 달치는 이미 받았다.
 *       다시 받으면 같은 달을 두 번 받는 것이고, 남은 일수만큼 비례 배분하면
 *       "왜 이 금액이냐"는 문의가 매번 생긴다. 요금제는 즉시 바뀌고 새 금액은 다음 청구일부터다
 * </ul>
 *
 * <p>협의 요금제({@code negotiable})는 여기로 오지 않는다 — 금액이 정해져 있지 않아
 * 자동 결제할 수 없다. 그쪽은 문의로 남는다.
 */
@Service
public class PlanChangeService {

    private static final Logger log = LoggerFactory.getLogger(PlanChangeService.class);

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final TenantBillingRepository billingRepository;
    private final BillingService billingService;

    public PlanChangeService(TenantRepository tenantRepository,
                             PlanRepository planRepository,
                             TenantBillingRepository billingRepository,
                             BillingService billingService) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.billingRepository = billingRepository;
        this.billingService = billingService;
    }

    @Transactional
    public PlanChangeResponse change(String planCode) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();

        Tenant tenant = tenantRepository.findById(user.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        Plan target = planRepository.findByCode(planCode.strip())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
        Plan current = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        if (!target.isSellable()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "판매 중인 요금제가 아닙니다");
        }
        if (target.isNegotiable()) {
            // 금액이 정해져 있지 않다. 자동 결제할 수 없으므로 문의로 보낸다.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "협의 요금제는 담당자 안내가 필요합니다. 문의를 남겨 주세요");
        }
        if (target.getId().equals(current.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "이미 쓰고 있는 요금제입니다");
        }
        if (billingRepository.findById(tenant.getId()).isEmpty()) {
            // 화면이 카드 등록으로 안내할 수 있도록 구분되는 코드를 준다.
            throw new BusinessException(ErrorCode.BILLING_KEY_MISSING);
        }

        // 무료에서 올라오는 경우에만 지금 받는다. 유료끼리는 이번 달치를 이미 받았다.
        boolean chargeNow = current.getMonthlyFee() <= 0 && target.getMonthlyFee() > 0;

        if (!chargeNow) {
            tenant.changePlan(target.getId());
            log.info("요금제를 바꿨습니다(청구 없음) — tenant={}, {} → {}, 다음 청구 {}",
                    tenant.getId(), current.getName(), target.getName(), tenant.getNextBillingDate());
            return new PlanChangeResponse(target.getName(), false, 0, null,
                    tenant.getNextBillingDate());
        }

        /*
         * 요금제를 <b>먼저</b> 바꾼다 — 청구 금액을 요금제에서 읽기 때문이다.
         * 결제가 실패하면 아래에서 되돌린다. 같은 트랜잭션이라 화면에는 옛 요금제로 남는다.
         */
        tenant.changePlan(target.getId());
        if (tenant.getStatus() == TenantStatus.TRIAL) {
            // 체험에서 올라왔다. 결제가 성공하면 정식 이용이다.
            tenant.activate();
        }

        BillingRecord record = billingService.charge(tenant.getId(), BillingService.currentPeriod());
        if (record.getStatus() != BillingStatus.PAID) {
            // **결제가 안 됐으면 요금제도 올리지 않는다.** 돈은 안 받고 한도만 열어주는 상태를
            // 만들지 않는다. 예외를 던져 트랜잭션 전체를 되돌린다.
            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                    record.getFailureReason() == null
                            ? "결제에 실패해 요금제를 바꾸지 못했습니다"
                            : record.getFailureReason());
        }

        tenant.startBillingOn(BusinessDay.today());
        log.info("요금제를 바꾸고 결제했습니다 — tenant={}, {} → {}, {}원",
                tenant.getId(), current.getName(), target.getName(), target.getMonthlyFee());

        return new PlanChangeResponse(target.getName(), true, target.getMonthlyFee(),
                record.getReceiptUrl(), tenant.getNextBillingDate());
    }
}
