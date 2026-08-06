package com.dabhaejwo.domain.billing.dto.response;

import com.dabhaejwo.domain.billing.entity.TenantBilling;

import java.time.OffsetDateTime;

/**
 * 등록된 결제수단.
 *
 * <p><b>빌링키는 절대 나가지 않는다.</b> 화면이 보여줄 것은 "어느 카드인지" 뿐이고,
 * 그건 마스킹된 번호로 충분하다.
 *
 * @param registered false 면 나머지 값은 전부 null 이다 — 화면은 등록 버튼을 띄운다
 */
public record BillingMethodResponse(boolean registered, String cardCompany,
                                    String cardNumberMasked, String cardType,
                                    OffsetDateTime registeredAt) {

    public static BillingMethodResponse none() {
        return new BillingMethodResponse(false, null, null, null, null);
    }

    public static BillingMethodResponse from(TenantBilling billing) {
        return new BillingMethodResponse(true, billing.getCardCompany(),
                billing.getCardNumberMasked(), billing.getCardType(), billing.getRegisteredAt());
    }
}
