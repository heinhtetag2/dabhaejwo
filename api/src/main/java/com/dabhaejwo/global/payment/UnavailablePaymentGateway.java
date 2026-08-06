package com.dabhaejwo.global.payment;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PG 미설정 시 쓰이는 구현.
 *
 * <p><b>가짜로 성공시키지 않는다.</b> 결제가 됐다고 응답하면 업체는 유료 전환이 끝난 줄 알고,
 * 우리는 받지 않은 돈을 받은 것으로 기록한다. 그 상태는 나중에 대조로만 발견된다.
 */
public class UnavailablePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(UnavailablePaymentGateway.class);

    @Override
    public BillingKeyIssued issueBillingKey(String customerKey, String authKey) {
        return reject("빌링키 발급");
    }

    @Override
    public PaymentResult charge(String billingKey, String customerKey, String orderId,
                                String orderName, int amountKrw) {
        return reject("결제");
    }

    private <T> T reject(String what) {
        log.error("{} 이 요청됐으나 PG 가 설정되지 않았습니다", what);
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "결제가 아직 연결되지 않았습니다. 담당자에게 문의해 주세요");
    }

    @Override
    public boolean available() {
        return false;
    }
}
