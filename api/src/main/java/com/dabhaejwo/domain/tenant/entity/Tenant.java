package com.dabhaejwo.domain.tenant.entity;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체(테넌트). 시스템상 격리 단위다.
 *
 * <p>setter 가 없다. 상태 변경은 의미 있는 메서드로만 하고, 전이 검증은 여기 안에서 한다.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "primary_domain", nullable = false)
    private String primaryDomain;

    @Column(name = "publishable_key", nullable = false, unique = true)
    private String publishableKey;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(nullable = false)
    private String currency;

    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    /**
     * 청구 기준일(1~31).
     *
     * <p><b>{@code nextBillingDate} 에서 파생하지 않는다.</b> 31일 기준인 업체가 2월에
     * 28일로 청구되면, 그 값에서 한 달을 더하는 순간 28일에 눌러앉아 매년 앞당겨진다.
     * 원래 기준일을 따로 기억해야 3월에 다시 31일로 돌아온다.
     */
    @Column(name = "billing_day")
    private Integer billingDay;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "churned_at")
    private OffsetDateTime churnedAt;

    /** 해지 후 벡터·문서를 삭제할 시각. 유예 기간은 cost_guards 설정값. */
    @Column(name = "purge_after")
    private OffsetDateTime purgeAfter;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Tenant() {
    }

    /**
     * 가입으로 만들어지는 업체. 무료 체험으로 시작한다.
     *
     * <p>{@code next_billing_date} 는 비워 둔다 — 체험 중에는 청구할 것이 없다.
     * 유료 전환 시 운영팀이 채운다 (tenant-public-plan.md §5.2).
     */
    public static Tenant startTrial(String name,
                                    String primaryDomain,
                                    String publishableKey,
                                    UUID planId,
                                    int trialDays) {
        Tenant tenant = new Tenant();
        tenant.name = name;
        tenant.primaryDomain = primaryDomain;
        tenant.publishableKey = publishableKey;
        tenant.planId = planId;
        tenant.status = TenantStatus.TRIAL;
        tenant.currency = "KRW";
        tenant.trialEndsAt = OffsetDateTime.now().plusDays(trialDays);
        tenant.createdAt = OffsetDateTime.now();
        tenant.updatedAt = tenant.createdAt;
        return tenant;
    }

    public void changePlan(UUID newPlanId) {
        requireNotChurned();
        this.planId = newPlanId;
        touch();
    }

    public void suspend() {
        transitionTo(TenantStatus.SUSPENDED);
        // 정지된 업체를 계속 청구하면 매일 실패가 쌓이고 카드사 쪽 거절 기록만 늘어난다.
        stopBilling();
    }

    /**
     * 정지 해제.
     *
     * <p><b>청구도 함께 되살린다.</b> {@link #suspend()} 가 청구일을 지우는데 여기서 되돌리지
     * 않으면, 결제 실패로 정지됐다가 해제된 업체는 <b>영영 청구되지 않는다</b> —
     * 서비스는 다시 쓰면서 돈은 안 내는 상태가 되고, 아무도 눈치채지 못한다.
     *
     * <p>오늘로 잡는 이유는 밀린 달이 있기 때문이다. 기준일은 그대로 두어 다음 달부터
     * 원래 날짜로 돌아간다.
     */
    public void activate() {
        transitionTo(TenantStatus.ACTIVE);
        if (billingDay != null && nextBillingDate == null) {
            this.nextBillingDate = LocalDate.now();
        }
    }

    /**
     * 해지. 벡터·문서는 즉시 지우지 않고 유예 기간 뒤에 지운다 —
     * 오조작으로 인한 해지를 되돌릴 여지를 남긴다.
     */
    public void churn(int purgeGraceDays) {
        transitionTo(TenantStatus.CHURNED);
        this.churnedAt = OffsetDateTime.now();
        this.purgeAfter = this.churnedAt.plusDays(purgeGraceDays);
    }

    public void extendTrial(int days) {
        if (status != TenantStatus.TRIAL) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "체험 중인 업체만 연장할 수 있습니다");
        }
        OffsetDateTime base = trialEndsAt == null ? OffsetDateTime.now() : trialEndsAt;
        this.trialEndsAt = base.plusDays(days);
        touch();
    }

    /**
     * 자동 청구를 시작한다. <b>결제한 날이 기준이 된다</b> — 8월 3일이면 매달 3일이다.
     *
     * <p>이미 기준일이 있으면 건드리지 않는다. 카드를 바꿨다고 청구일이 옮겨가면
     * 업체는 언제 빠져나갈지 예측할 수 없다.
     */
    public void startBillingOn(LocalDate firstDate) {
        if (billingDay == null) {
            this.billingDay = firstDate.getDayOfMonth();
        }
        this.nextBillingDate = firstDate;
        touch();
    }

    /** 청구 성공. 다음 달 같은 날로 옮긴다 — 그 달에 없는 날짜면 말일로 당긴다. */
    public void advanceBilling() {
        LocalDate base = nextBillingDate == null ? LocalDate.now() : nextBillingDate;
        this.nextBillingDate = onDay(base.plusMonths(1), billingDay);
        touch();
    }

    /** 청구 실패. 며칠 뒤 다시 시도한다. */
    public void retryBillingOn(LocalDate retryDate) {
        this.nextBillingDate = retryDate;
        touch();
    }

    /** 더 이상 청구하지 않는다(해지·정지). 남겨 두면 배치가 매일 실패를 반복한다. */
    public void stopBilling() {
        this.nextBillingDate = null;
        touch();
    }

    /**
     * 그 달의 기준일. 없는 날짜면 말일로 당긴다.
     *
     * <p>1/31 → 2/28(윤년이면 29) → 3/31. 기준일을 따로 들고 있어야 3월에 되돌아온다.
     */
    static LocalDate onDay(LocalDate month, Integer day) {
        if (day == null) {
            return month;
        }
        return month.withDayOfMonth(Math.min(day, month.lengthOfMonth()));
    }

    public Integer getBillingDay() {
        return billingDay;
    }

    private void transitionTo(TenantStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    status + " 에서 " + target + " 로는 바꿀 수 없습니다");
        }
        this.status = target;
        touch();
    }

    private void requireNotChurned() {
        if (status == TenantStatus.CHURNED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "해지된 업체는 변경할 수 없습니다");
        }
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrimaryDomain() {
        return primaryDomain;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public UUID getPlanId() {
        return planId;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getTrialEndsAt() {
        return trialEndsAt;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
