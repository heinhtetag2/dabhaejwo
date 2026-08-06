package com.dabhaejwo.domain.billing.service;

import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.common.BusinessDay;
import com.dabhaejwo.global.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 자동 청구.
 *
 * <p>매일 한 번 돌면서 <b>청구일이 오늘까지인</b> 업체를 찾아 청구한다.
 * "결제한 날"이 기준이라 8월 3일에 결제한 업체는 9월 3일에 다시 청구된다.
 *
 * <p>지난 날짜도 집는다({@code <=}) — 배치가 하루 멈췄다고 그 날 청구가 사라지면 안 된다.
 *
 * <p><b>이중 청구를 막는 것이 이 클래스의 전부다.</b> 세 겹으로 막는다:
 * <ul>
 *   <li>주문번호가 업체 + 청구월로 결정된다 — 몇 번을 돌려도 같은 값이다
 *   <li>토스에 그 값을 멱등키로 보낸다 — 같은 키면 앞선 결과를 그대로 돌려준다
 *   <li>{@code billing_records.order_id} 에 UNIQUE, 이미 {@code PAID} 면 건너뛴다
 * </ul>
 *
 * <p>그래서 인스턴스가 여럿이어도 돈이 두 번 나가지는 않는다. 다만 <b>헛수고는 한다</b> —
 * 잠금이 없어 같은 업체를 둘이 집어간다. 확장할 때 잠금을 붙여야 한다
 * ({@code docs/IMPROVEMENTS.md}).
 */
@Component
public class BillingWorker {

    private static final Logger log = LoggerFactory.getLogger(BillingWorker.class);

    /** 한 번에 처리할 업체 수. 결제는 왕복이 길어 한 주기에 다 하려 들지 않는다. */
    private static final int BATCH = 50;

    private final TenantRepository tenantRepository;
    private final BillingService billingService;
    private final PaymentGateway gateway;

    public BillingWorker(TenantRepository tenantRepository,
                         BillingService billingService,
                         PaymentGateway gateway) {
        this.tenantRepository = tenantRepository;
        this.billingService = billingService;
        this.gateway = gateway;
    }

    /**
     * 매일 새벽 4시(한국 시간).
     *
     * <p>사람이 적은 시간을 고른다 — 결제 실패 메일이 한밤중에 가는 것보다, 아침에 확인하고
     * 바로 고칠 수 있는 편이 낫다. 자정 직후가 아닌 이유는 그 시각에 다른 집계가 몰려서다.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void run() {
        if (!gateway.available()) {
            // PG 미설정. 매일 실패를 쌓는 대신 한 줄만 남긴다.
            log.debug("PG 가 설정되지 않아 자동 청구를 건너뜁니다");
            return;
        }
        chargeDue(BusinessDay.today());
    }

    /**
     * 오늘까지 청구할 곳을 처리한다.
     *
     * <p>업체마다 <b>독립된 트랜잭션</b>이다({@code BillingService#charge}) — 한 곳이 터져도
     * 나머지는 청구된다. 여기서 한 번에 묶으면 마지막 업체의 실패가 앞선 성공까지 되감는다.
     */
    @Transactional(readOnly = true)
    public int chargeDue(LocalDate today) {
        List<Tenant> due = tenantRepository.findDueForBilling(today, Limit.of(BATCH));
        if (due.isEmpty()) {
            return 0;
        }
        log.info("자동 청구 대상 {}곳", due.size());

        int done = 0;
        for (Tenant tenant : due) {
            try {
                billingService.charge(tenant.getId(), BillingService.currentPeriod());
                done++;
            } catch (RuntimeException e) {
                // 한 곳이 터져도 나머지는 계속한다. 실패 사유는 charge 안에서 기록된다.
                log.error("자동 청구 중 예기치 못한 오류 — tenant={}", tenant.getId(), e);
            }
        }
        return done;
    }
}
