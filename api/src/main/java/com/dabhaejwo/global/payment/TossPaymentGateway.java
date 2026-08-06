package com.dabhaejwo.global.payment;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 자동결제(빌링).
 *
 * <p>카드를 한 번 등록해 빌링키를 받아두고 매달 그 키로 청구한다.
 * <b>카드번호는 우리가 받지 않는다</b> — 결제창이 토스와 직접 주고받고, 우리에게는 키만 온다.
 * 카드 정보를 우리 서버가 만지지 않는 것이 이 방식을 택한 가장 큰 이유다.
 *
 * <p>인증은 시크릿 키의 Basic 인증이다. 토스 규약상 <b>키 뒤에 콜론을 붙여</b> base64 한다
 * (비밀번호가 빈 문자열인 Basic 인증). 콜론을 빠뜨리면 401 이 나는데 원인이 잘 안 보인다.
 */
public class TossPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentGateway.class);

    private static final String BASE_URL = "https://api.tosspayments.com";
    /** 멱등키 헤더. 같은 값으로 다시 부르면 토스가 앞선 결과를 그대로 돌려준다. */
    private static final String IDEMPOTENCY_HEADER = "Idempotent-Key";

    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient client;

    public TossPaymentGateway(String secretKey) {
        String basic = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.client = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public BillingKeyIssued issueBillingKey(String customerKey, String authKey) {
        Map<String, Object> response = post("/v1/billing/authorizations/issue",
                Map.of("customerKey", customerKey, "authKey", authKey), null);

        String billingKey = string(response, "billingKey");
        if (billingKey == null || billingKey.isBlank()) {
            // 200 인데 키가 없다면 응답 형상이 바뀐 것이다. 빈 값을 저장하면
            // 카드가 등록된 것처럼 보이고 청구할 때 비로소 터진다.
            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                    "카드를 등록하지 못했습니다. 잠시 후 다시 시도해 주세요");
        }

        Map<String, Object> card = mapOf(response, "card");
        return new BillingKeyIssued(billingKey,
                string(card, "issuerCode"), string(card, "number"), string(card, "cardType"));
    }

    @Override
    public PaymentResult charge(String billingKey, String customerKey, String orderId,
                                String orderName, int amountKrw) {
        try {
            // 멱등키로 orderId 를 쓴다. 네트워크가 끊겨 재시도해도 두 번 청구되지 않는다.
            Map<String, Object> response = post("/v1/billing/" + billingKey,
                    Map.of("customerKey", customerKey,
                            "amount", amountKrw,
                            "orderId", orderId,
                            "orderName", orderName),
                    orderId);

            String status = string(response, "status");
            boolean approved = "DONE".equals(status);
            if (!approved) {
                log.warn("결제가 승인되지 않았습니다 — orderId={}, status={}", orderId, status);
            }
            return new PaymentResult(approved,
                    string(response, "paymentKey"),
                    string(mapOf(response, "receipt"), "url"),
                    string(response, "method"),
                    approved ? null : status,
                    approved ? null : "승인되지 않았습니다 (" + status + ")");

        } catch (BusinessException e) {
            // 카드사 거절처럼 <b>업체에게 알려야 하는</b> 실패다. 예외로 올리지 않고
            // 결과로 돌려준다 — 호출부가 billing_records 에 FAILED 로 남겨야 하기 때문이다.
            log.info("결제 실패 — orderId={}, {}", orderId, e.getMessage());
            return new PaymentResult(false, null, null, null, e.errorCode().name(), e.getMessage());
        }
    }

    private Map<String, Object> post(String path, Object body, String idempotencyKey) {
        try {
            RestClient.RequestBodySpec spec = client.post().uri(path);
            if (idempotencyKey != null) {
                spec = spec.header(IDEMPOTENCY_HEADER, idempotencyKey);
            }
            Map<String, Object> response = spec.body(body).retrieve().body(MAP);
            if (response == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED, "결제사가 빈 응답을 돌려주었습니다");
            }
            return response;

        } catch (RestClientResponseException e) {
            // 토스는 {code, message} 로 사유를 준다. 그 message 는 카드사 문구라
            // 그대로 보여주는 편이 낫다 — "카드 한도 초과" 같은 말은 업체가 바로 조치할 수 있다.
            String message = extractMessage(e.getResponseBodyAsString());
            log.warn("토스 호출 실패 path={} status={} message={}", path, e.getStatusCode(), message);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, message);

        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            // 네트워크 오류 등. 본문에 키가 섞여 나갈 수 있어 메시지를 그대로 노출하지 않는다.
            log.error("토스 호출 실패 path={} type={}", path, e.getClass().getSimpleName(), e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                    "결제사에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요");
        }
    }

    /** 응답 본문에서 사람이 읽을 문구만 꺼낸다. 파싱에 실패해도 일반 문구로 떨어진다. */
    private String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "결제를 처리하지 못했습니다";
        }
        int at = body.indexOf("\"message\"");
        if (at < 0) {
            return "결제를 처리하지 못했습니다";
        }
        int start = body.indexOf('"', body.indexOf(':', at)) + 1;
        int end = body.indexOf('"', start);
        return (start > 0 && end > start) ? body.substring(start, end) : "결제를 처리하지 못했습니다";
    }

    private String string(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOf(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @Override
    public boolean available() {
        return true;
    }
}
