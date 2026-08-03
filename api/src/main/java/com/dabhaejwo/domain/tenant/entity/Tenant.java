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

    public void changePlan(UUID newPlanId) {
        requireNotChurned();
        this.planId = newPlanId;
        touch();
    }

    public void suspend() {
        transitionTo(TenantStatus.SUSPENDED);
    }

    public void activate() {
        transitionTo(TenantStatus.ACTIVE);
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
