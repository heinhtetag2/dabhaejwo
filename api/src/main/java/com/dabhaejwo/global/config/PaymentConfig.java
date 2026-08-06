package com.dabhaejwo.global.config;

import com.dabhaejwo.global.payment.PaymentGateway;
import com.dabhaejwo.global.payment.TossPaymentGateway;
import com.dabhaejwo.global.payment.UnavailablePaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 결제 대행사 선택.
 *
 * <p>키가 없으면 {@link UnavailablePaymentGateway} 다 — <b>가짜 성공으로 대체하지 않는다.</b>
 * 결제가 됐다고 응답하면 업체는 유료 전환이 끝난 줄 알고, 우리는 받지 않은 돈을 받은 것으로
 * 기록한다. 그 상태는 나중에 대조로만 발견된다.
 */
@Configuration
public class PaymentConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);

    @Bean
    PaymentGateway paymentGateway(AppProperties properties) {
        AppProperties.Payment config = properties.payment();
        if (!config.configured()) {
            log.warn("PG 가 설정되지 않았습니다 — 카드 등록·결제가 거절됩니다");
            return new UnavailablePaymentGateway();
        }
        log.info("토스페이먼츠 연결됨 — {}", config.live() ? "라이브 키" : "테스트 키");
        return new TossPaymentGateway(config.secretKey());
    }
}
