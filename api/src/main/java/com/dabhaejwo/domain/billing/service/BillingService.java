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
import com.dabhaejwo.domain.member.entity.TenantMember;
import com.dabhaejwo.domain.member.repository.TenantMemberRepository;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.crypto.SecretCipher;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.notify.Greeting;
import com.dabhaejwo.global.notify.MailTemplate;
import com.dabhaejwo.global.notify.Mailer;
import com.dabhaejwo.global.payment.PaymentGateway;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import com.dabhaejwo.global.security.TenantMemberRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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

    /** 재시도 간격과 횟수. 3일 × 3회 = 9일 동안 고칠 기회를 준다. */
    private static final int RETRY_INTERVAL_DAYS = 3;
    private static final int MAX_ATTEMPTS = 3;

    private final TenantBillingRepository billingRepository;
    private final BillingRecordRepository recordRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final PaymentGateway gateway;
    private final SecretCipher cipher;
    private final TenantMemberRepository memberRepository;
    private final Mailer mailer;
    private final AppProperties.Mail mailConfig;

    public BillingService(TenantBillingRepository billingRepository,
                          BillingRecordRepository recordRepository,
                          TenantRepository tenantRepository,
                          PlanRepository planRepository,
                          PaymentGateway gateway,
                          SecretCipher cipher,
                          TenantMemberRepository memberRepository,
                          Mailer mailer,
                          AppProperties properties) {
        this.memberRepository = memberRepository;
        this.mailer = mailer;
        this.mailConfig = properties.mail();
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

        // 청구 일정을 건다. **체험 중이면 체험이 끝나는 날**이 첫 결제일이다 —
        // 지금 받으면 남은 체험 기간을 뺏는 셈이다. 그 날이 이후 매달의 기준일이 된다.
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        if (tenant.getNextBillingDate() == null) {
            tenant.startBillingOn(firstBillingDate(tenant));
        }

        log.info("결제수단을 등록했습니다 — tenant={}, card={}, 첫 청구 {}",
                tenantId, issued.cardNumberMasked(), tenant.getNextBillingDate());
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
            tenant.advanceBilling();
            log.info("결제 성공 — tenant={}, period={}, {}원, 다음 {}",
                    tenantId, month, amount, tenant.getNextBillingDate());
            notifyPaid(tenant, amount, result.receiptUrl());
        } else {
            record.markFailed(orderId, result.failureReason());
            applyFailurePolicy(tenant, record, result.failureReason());
        }
        return record;
    }

    /**
     * 실패 정책 — <b>3일 간격 3회 재시도, 그래도 안 되면 일시정지.</b>
     *
     * <p>즉시 정지하면 카드 재발급이나 한도 같은 일시적 사유에도 방문자 상담이 당장 끊긴다.
     * 반대로 계속 두면 미수금이 쌓이는 동안 모델 원가는 계속 나간다. 9일이 그 사이다.
     *
     * <p>재시도는 <b>같은 달·같은 주문번호</b>로 간다 — 그래서 중복 청구가 되지 않는다.
     */
    private void applyFailurePolicy(Tenant tenant, BillingRecord record, String reason) {
        if (record.getAttempts() >= MAX_ATTEMPTS) {
            tenant.suspend();
            log.warn("결제 {}회 실패로 일시정지합니다 — tenant={}, 사유={}",
                    record.getAttempts(), tenant.getId(), reason);
            notifyFailure(tenant, reason, true);
            return;
        }
        LocalDate retryOn = BusinessDay.today().plusDays(RETRY_INTERVAL_DAYS);
        tenant.retryBillingOn(retryOn);
        log.warn("결제 실패 — tenant={}, {}회차, 사유={}, 재시도 {}",
                tenant.getId(), record.getAttempts(), reason, retryOn);
        notifyFailure(tenant, reason, false);
    }

    /**
     * 결제 알림.
     *
     * <p>메일이 실패해도 <b>결제 결과를 되돌리지 않는다</b> — 돈은 이미 오갔다.
     * 초대·인증과 반대다: 그쪽은 메일이 안 나가면 기능이 성립하지 않지만, 여기는 이미 끝났다.
     */
    private void notifyPaid(Tenant tenant, int amount, String receiptUrl) {
        ownerEmail(tenant).ifPresent(email -> safeSend(email,
                "[답해줘] 결제가 완료되었습니다",
                MailTemplate.build(Greeting.of(null, email), "결제 완료",
                        "%s 이용료 %,d원이 결제되었습니다.".formatted(tenant.getName(), amount),
                        null,
                        receiptUrl == null ? null : new MailTemplate.Cta("영수증 보기", receiptUrl),
                        List.of("결제 내역은 대시보드의 요금제 화면에서도 확인하실 수 있습니다."))));
    }

    private void notifyFailure(Tenant tenant, String reason, boolean suspended) {
        ownerEmail(tenant).ifPresent(email -> safeSend(email,
                suspended ? "[답해줘] 결제 실패로 서비스가 중단되었습니다" : "[답해줘] 결제가 실패했습니다",
                MailTemplate.build(Greeting.of(null, email),
                        suspended ? "서비스가 중단되었습니다" : "결제가 실패했습니다",
                        reason == null ? "카드 결제가 처리되지 않았습니다." : reason,
                        null,
                        new MailTemplate.Cta("결제수단 변경하기", mailConfig.appBaseUrl() + "/app/plan"),
                        suspended
                                ? List.of("새 카드를 등록하시면 담당자 확인 후 곧바로 다시 이용하실 수 있습니다.")
                                : List.of("%d일 뒤에 다시 시도합니다. 그때까지 카드를 확인해 주세요."
                                        .formatted(RETRY_INTERVAL_DAYS),
                                        "그동안 챗봇은 정상 동작합니다."))));
    }

    /** 소유자 주소. 없으면 보내지 않는다 — 아무에게나 결제 메일을 보내지 않는다. */
    private Optional<String> ownerEmail(Tenant tenant) {
        return memberRepository.findAllByTenantIdOrderByCreatedAtAsc(tenant.getId()).stream()
                .filter(member -> member.getRole() == TenantMemberRole.OWNER)
                .map(TenantMember::getEmail)
                .findFirst();
    }

    private void safeSend(String to, String subject, MailTemplate.Body body) {
        try {
            mailer.send(to, subject, body);
        } catch (RuntimeException e) {
            log.warn("결제 알림 메일을 보내지 못했습니다 — to={}", to, e);
        }
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

    /**
     * 첫 청구일.
     *
     * <p>체험이 남아 있으면 <b>체험 종료일</b>, 이미 끝났으면 오늘이다.
     * 체험 중에 카드를 등록했다고 바로 받으면 남은 기간을 뺏는 것이다.
     */
    private LocalDate firstBillingDate(Tenant tenant) {
        LocalDate today = BusinessDay.today();
        if (tenant.getTrialEndsAt() == null) {
            return today;
        }
        LocalDate trialEnd = tenant.getTrialEndsAt().atZoneSameInstant(BusinessDay.ZONE).toLocalDate();
        return trialEnd.isAfter(today) ? trialEnd : today;
    }

    /** 이번 달. 청구 기준월은 업체의 하루를 따른다 ({@link BusinessDay}). */
    public static LocalDate currentPeriod() {
        OffsetDateTime now = BusinessDay.startOfToday();
        return now.toLocalDate().withDayOfMonth(1);
    }
}
