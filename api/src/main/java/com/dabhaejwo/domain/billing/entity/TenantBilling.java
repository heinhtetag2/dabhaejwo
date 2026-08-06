package com.dabhaejwo.domain.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체의 결제수단(빌링키).
 *
 * <p><b>카드번호를 갖지 않는다.</b> 결제창이 토스와 직접 주고받고 우리에게는 키만 온다.
 * 화면에 보여줄 마스킹 값만 따로 받아 저장한다.
 *
 * <p>빌링키는 <b>그 자체로 돈을 뺄 수 있는 값</b>이라 암호문으로만 들고 있는다.
 * 평문 컬럼을 두지 않는 것이 설계다 — 두면 언젠가 로그나 응답에 섞여 나간다.
 *
 * <p>업체당 하나다. 여러 장을 허용하면 "어느 카드로 청구했나"가 매달 달라지고,
 * 실패했을 때 어느 것을 고쳐야 하는지도 흐려진다.
 */
@Entity
@Table(name = "tenant_billing")
public class TenantBilling {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "billing_key_cipher", nullable = false)
    private String billingKeyCipher;

    @Column(name = "customer_key", nullable = false)
    private String customerKey;

    @Column(name = "card_company")
    private String cardCompany;

    @Column(name = "card_number_masked")
    private String cardNumberMasked;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "registered_by")
    private UUID registeredBy;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected TenantBilling() {
    }

    public static TenantBilling of(UUID tenantId, String customerKey, String billingKeyCipher,
                                   String cardCompany, String cardNumberMasked, String cardType,
                                   UUID registeredBy) {
        TenantBilling billing = new TenantBilling();
        billing.tenantId = tenantId;
        billing.customerKey = customerKey;
        billing.billingKeyCipher = billingKeyCipher;
        billing.cardCompany = cardCompany;
        billing.cardNumberMasked = cardNumberMasked;
        billing.cardType = cardType;
        billing.registeredBy = registeredBy;
        billing.registeredAt = OffsetDateTime.now();
        billing.updatedAt = billing.registeredAt;
        return billing;
    }

    /** 카드를 바꿨다. 새 빌링키로 갈아끼운다 — 이전 키는 남기지 않는다. */
    public void replace(String billingKeyCipher, String cardCompany, String cardNumberMasked,
                        String cardType, UUID changedBy) {
        this.billingKeyCipher = billingKeyCipher;
        this.cardCompany = cardCompany;
        this.cardNumberMasked = cardNumberMasked;
        this.cardType = cardType;
        this.registeredBy = changedBy;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getBillingKeyCipher() {
        return billingKeyCipher;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public String getCardCompany() {
        return cardCompany;
    }

    public String getCardNumberMasked() {
        return cardNumberMasked;
    }

    public String getCardType() {
        return cardType;
    }

    public OffsetDateTime getRegisteredAt() {
        return registeredAt;
    }
}
