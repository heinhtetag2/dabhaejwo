package com.dabhaejwo.domain.plan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 요금제.
 *
 * <p><b>삭제 메서드가 없다.</b> 판매를 멈출 뿐이다 — 기존 계약 업체가 남아 있기 때문이다.
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** 원 단위. 협의가(기업 요금제)는 0 + negotiable=true. */
    @Column(name = "monthly_fee", nullable = false)
    private int monthlyFee;

    @Column(nullable = false)
    private boolean negotiable;

    @Column(name = "conv_limit", nullable = false)
    private int convLimit;

    @Column(name = "doc_limit", nullable = false)
    private int docLimit;

    @Column(nullable = false)
    private boolean sellable;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Plan() {
    }

    /** 판매 중단. 삭제 대신 이것만 한다. */
    public void stopSelling() {
        this.sellable = false;
    }

    public void resumeSelling() {
        this.sellable = true;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getMonthlyFee() {
        return monthlyFee;
    }

    public boolean isNegotiable() {
        return negotiable;
    }

    public int getConvLimit() {
        return convLimit;
    }

    public int getDocLimit() {
        return docLimit;
    }

    public boolean isSellable() {
        return sellable;
    }
}
