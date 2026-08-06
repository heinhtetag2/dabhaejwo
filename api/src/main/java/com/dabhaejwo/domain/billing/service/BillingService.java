package com.dabhaejwo.domain.billing.service;

import com.dabhaejwo.domain.billing.dto.request.BillingAuthRequest;
import com.dabhaejwo.domain.billing.dto.response.BillingMethodResponse;
import com.dabhaejwo.domain.billing.entity.BillingRecord;
import com.dabhaejwo.domain.billing.entity.BillingStatus;
import com.dabhaejwo.domain.billing.entity.TenantBilling;
import com.dabhaejwo.domain.billing.repository.BillingRecordRepository;
import com.dabhaejwo.domain.billing.repository.TenantBillingRepository;
import com.dabhaejwo.domain.plan.entity.Plan;
import com.dabhaejwo.domain.plan.repository.PlanRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.common.BusinessDay;
import com.dabhaejwo.global.crypto.SecretCipher;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.payment.PaymentGateway;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 결제수단 등록과 청구.
 *
 * <p>지켜야 할 것 넷 — 전부 <b>돈이 잘못 나가는 것을 막는</b> 장치다:
 * <ul>
 *   <li><b>금액은 서버가 정한다.</b> 클라이언트가 보낸 금액으로 청구하면 1원짜리 요청이 온다
 *   <li><b>주문번호는 업체 + 청구월로 결정된다.</b> 재시도해도 같은 값이라 두 번 청구되지 않는다
 *   <li><b>빌링키는 암호문으로만 저장한다.</b> 그 자체로 돈을 뺄 수 있는 값이다
 *   <li><b>실패도 기록한다.</b> 남기지 않으면 왜 못 받았는지 알 방법이 없다
 * </ul>
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final TenantBillingRepository billingRepository;
    private final BillingRecordRepository recordRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final PaymentGateway gateway;
    private final SecretCipher cipher;

    public BillingService(TenantBillingRepository billingRepository,
                          BillingRecordRepository recordRepository,
                          TenantRepository tenantRepository,
                          PlanRepository planRepository,
                          PaymentGateway gateway,
                          SecretCipher cipher) {
        this.billingRepository = billingRepository;
        this.recordRepository = recordRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.gateway = gateway;
        this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public BillingMethodResponse current() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return billingRepository.findById(tenantId)
                .map(BillingMethodResponse::from)
                .orElseGet(BillingMethodResponse::none);
    }

    /**
     * 결제창에서 받은 인증키를 빌링키로 바꿔 저장한다.
     *
     * <p>결제·팀원과 같은 등급이라 <b>소유자만</b> 할 수 있다. 대리 접속 중에는 막는다 —
     * 운영자가 업체 카드를 등록하는 일은 없어야 한다.
     */
    @Transactional
    public BillingMethodResponse register(BillingAuthRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();

        UUID tenantId = user.tenantId();
        String customerKey = customerKeyOf(tenantId);

        // 클라이언트가 보낸 customerKey 를 신뢰하지 않는다. 다르면 남의 업체에 카드를 붙이는 셈이다.
        if (!customerKey.equals(request.customerKey())) {
            log.warn("customerKey 가 토큰의 업체와 다릅니다 — tenant={}", tenantId);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        requireCipher();

        PaymentGateway.BillingKeyIssued issued =
                gateway.issueBillingKey(customerKey, request.authKey());
        String encrypted = cipher.encrypt(issued.billingKey());

        TenantBilling billing = billingRepository.findById(tenantId)
                .map(existing -> {
                    existing.replace(encrypted, issued.cardCompany(), issued.cardNumberMasked(),
                            issued.cardType(), user.memberId());
                    return existing;
                })
                .orElseGet(() -> billingRepository.save(TenantBilling.of(
                        tenantId, customerKey, encrypted, issued.cardCompany(),
                        issued.cardNumberMasked(), issued.cardType(), user.memberId())));

        log.info("결제수단을 등록했습니다 — tenant={}, card={}", tenantId, issued.cardNumberMasked());
        return BillingMethodResponse.from(billing);
    }

    /**
     * 결제수단 삭제.
     *
     * <p>업체가 스스로 뺄 수 있어야 한다 — 뺄 수 없는 결제수단은 그 자체로 분쟁거리다.
     * 다만 다음 청구가 실패하게 되므로 화면이 그 사실을 먼저 알린다.
     */
    @Transactional
    public void remove() {
        AuthPrincipal.TenantUser user = CurrentAuth.requireOwner();
        CurrentAuth.rejectIfImpersonating();

        billingRepository.findById(user.tenantId()).ifPresent(billing -> {
            billingRepository.delete(billing);
            log.info("결제수단을 삭제했습니다 — tenant={}", user.tenantId());
        });
    }

    /**
     * 한 업체의 이번 달 요금을 청구한다.
     *
     * <p>배치와 운영자 수동 실행이 같은 경로를 쓴다 — 두 벌로 두면 한쪽만 고쳐진다.
     *
     * @return 청구 기록. 실패해도 기록은 남는다
     */
    @Transactional
    public BillingRecord charge(UUID tenantId, LocalDate period) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        Plan plan = planRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        // 금액은 <b>요금제에서</b> 온다. 클라이언트가 보낸 값으로 청구하면 1원짜리 요청이 온다.
        int amount = plan.getMonthlyFee();
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "무료 요금제는 청구하지 않습니다");
        }

        LocalDate month = period.withDayOfMonth(1);
        String orderId = orderId(tenantId, month);

        // 이미 받은 달을 다시 청구하지 않는다. 멱등키가 토스 쪽 보장이라면 이건 우리 쪽 보장이다.
        Optional<BillingRecord> existing = recordRepository.findByTenantIdAndPeriod(tenantId, month);
        if (existing.isPresent() && existing.get().getStatus() == BillingStatus.PAID) {
            log.info("이미 청구된 달입니다 — tenant={}, period={}", tenantId, month);
            return existing.get();
        }

        TenantBilling billing = billingRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BILLING_KEY_MISSING));
        requireCipher();

        PaymentGateway.PaymentResult result = gateway.charge(
                cipher.decrypt(billing.getBillingKeyCipher()),
                billing.getCustomerKey(),
                orderId,
                "%s %s 이용료".formatted(tenant.getName(), month.getYear() + "년 " + month.getMonthValue() + "월"),
                amount);

        BillingRecord record = existing.orElseGet(() ->
                recordRepository.save(BillingRecord.of(tenantId, month, amount, BillingStatus.PENDING)));

        if (result.approved()) {
            record.markPaid(orderId, result.paymentKey(), result.receiptUrl(), result.method());
            log.info("결제 성공 — tenant={}, period={}, {}원", tenantId, month, amount);
        } else {
            record.markFailed(orderId, result.failureReason());
            log.warn("결제 실패 — tenant={}, period={}, 사유={}", tenantId, month, result.failureReason());
        }
        return record;
    }

    /**
     * 주문번호.
     *
     * <p>업체와 청구월만으로 정해진다 — <b>재시도해도 같은 값</b>이라 중복 청구가 막힌다.
     * 무작위로 만들면 네트워크가 끊겼을 때 두 번 청구된다.
     */
    static String orderId(UUID tenantId, LocalDate month) {
        return "dbz-%s-%d%02d".formatted(
                tenantId.toString().substring(0, 8), month.getYear(), month.getMonthValue());
    }

    /** 토스에 넘길 구매자 식별자. 업체 id 를 그대로 쓴다 — 추측 가능한 연번이 아니다. */
    static String customerKeyOf(UUID tenantId) {
        return tenantId.toString();
    }

    /**
     * 암호화 키가 없으면 빌링키를 저장할 수 없다.
     *
     * <p>평문으로 떨어뜨리지 않는다 — 그러면 DB 를 읽는 것만으로 남의 카드에 청구할 수 있다.
     */
    private void requireCipher() {
        if (!cipher.available()) {
            throw new BusinessException(ErrorCode.ENCRYPTION_UNAVAILABLE,
                    "암호화 키가 설정되지 않아 결제수단을 저장할 수 없습니다");
        }
    }

    /** 이번 달. 청구 기준월은 업체의 하루를 따른다 ({@link BusinessDay}). */
    public static LocalDate currentPeriod() {
        OffsetDateTime now = BusinessDay.startOfToday();
        return now.toLocalDate().withDayOfMonth(1);
    }
}
