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

    /**
     * 만들 수 있는 서비스 수.
     *
     * <p><b>생성 시점에만 검사한다.</b> 내려도 기존 서비스를 강제로 지우지 않는다 —
     * 이미 남의 사이트에서 도는 위젯을 요금제 변경으로 조용히 죽이면 방문자에게는 장애로 보인다.
     */
    @Column(name = "bot_limit", nullable = false)
    private int botLimit;

    @Column(nullable = false)
    private boolean sellable;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Plan() {
    }

    public static Plan create(String code, String name, int monthlyFee, boolean negotiable,
                              int convLimit, int docLimit, int botLimit, int sortOrder) {
        Plan plan = new Plan();
        plan.code = code;
        plan.name = name;
        plan.monthlyFee = monthlyFee;
        plan.negotiable = negotiable;
        plan.convLimit = convLimit;
        plan.botLimit = botLimit;
        plan.docLimit = docLimit;
        plan.sellable = true;
        plan.sortOrder = sortOrder;
        plan.createdAt = OffsetDateTime.now();
        return plan;
    }

    /**
     * 수정. {@code code} 는 인자에 없다 — 코드가 이 값으로 요금제를 찾으므로 바뀌면
     * 참조가 조용히 끊긴다.
     */
    public void update(String newName, int newMonthlyFee, boolean newNegotiable,
                       int newConvLimit, int newDocLimit, boolean newSellable, int newBotLimit) {
        this.name = newName;
        this.monthlyFee = newMonthlyFee;
        this.negotiable = newNegotiable;
        this.convLimit = newConvLimit;
        // 내려도 기존 서비스를 강제로 지우지 않는다 — 생성 시점에만 검사한다.
        this.botLimit = newBotLimit;
        this.docLimit = newDocLimit;
        this.sellable = newSellable;
    }

    /** 판매 중단. 삭제 대신 이것만 한다. */
    public void stopSelling() {
        this.sellable = false;
    }

    public void resumeSelling() {
        this.sellable = true;
    }

    public int getSortOrder() {
        return sortOrder;
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

    public int getBotLimit() {
        return botLimit;
    }

    public boolean isSellable() {
        return sellable;
    }
}
