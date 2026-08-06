package com.dabhaejwo.global.payment;

/**
 * 결제 대행사(PG).
 *
 * <p>인터페이스로 두는 이유는 공급사를 바꿀 수 있어서가 아니라, <b>미설정 상태를
 * 타입으로 표현하기 위해서</b>다. 키가 없으면 {@link UnavailablePaymentGateway} 가
 * 명시적으로 거절한다 — 결제는 조용히 실패하면 안 되는 대표적인 기능이다.
 */
public interface PaymentGateway {

    /**
     * 결제창에서 받은 일회용 인증키를 <b>빌링키</b>로 바꾼다.
     *
     * <p>빌링키는 그 자체로 돈을 뺄 수 있는 값이다. 호출부는 반드시 암호화해 저장한다.
     */
    BillingKeyIssued issueBillingKey(String customerKey, String authKey);

    /**
     * 빌링키로 청구한다.
     *
     * @param orderId 재시도해도 같은 값이어야 한다 — 멱등키로 함께 쓰이므로
     *                이중 청구를 막는 유일한 장치다
     */
    PaymentResult charge(String billingKey, String customerKey, String orderId,
                         String orderName, int amountKrw);

    /** 설정이 없으면 false. 이때는 결제 시도 자체를 하지 않는다. */
    boolean available();

    /**
     * @param cardNumberMasked 토스가 마스킹해서 준 값이다. <b>원본 카드번호는 우리가 받지 않는다.</b>
     */
    record BillingKeyIssued(String billingKey, String cardCompany, String cardNumberMasked,
                            String cardType) {
    }

    /**
     * @param approved  성공 여부. false 면 {@code failureReason} 에 사유가 있다
     * @param receiptUrl 영수증. 업체가 화면에서 바로 열 수 있어야 한다
     */
    record PaymentResult(boolean approved, String paymentKey, String receiptUrl,
                         String method, String failureCode, String failureReason) {
    }
}
