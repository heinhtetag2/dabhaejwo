package com.dabhaejwo.global.config;

import com.dabhaejwo.global.llm.LlmProviderName;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * 설정값은 전부 여기로 모은다. 매직 넘버·금액·계산식을 코드에 하드코딩하지 않는다.
 *
 * <p>모델명과 단가는 여기 없다 — {@code model_prices} 테이블에 있다.
 * 공급사 가격 변동과 환율에 대응해야 하므로 재배포 없이 바뀔 수 있어야 한다.
 */
@ConfigurationProperties(prefix = "dabhaejwo")
public record AppProperties(
        Auth auth,
        Ops ops,
        Llm llm,
        Storage storage,
        // `notify` 는 Object.notify() 와 충돌해 record 컴포넌트명으로 쓸 수 없다.
        Notification notification,
        Cors cors
) {

    /**
     * 콘솔·대시보드가 브라우저에서 API 를 부를 수 있게 허용할 출처.
     *
     * <p><b>위젯은 여기 없다.</b> 위젯은 남의 사이트 어디서든 호출되므로 출처를 미리 알 수 없고,
     * 실제 차단은 {@code WidgetKeyAuthFilter} 가 테넌트의 등록 주소로 한다 — CORS 는
     * 브라우저가 응답을 읽게 할지의 문제이지 보안 경계가 아니다.
     */
    public record Cors(@DefaultValue("") List<String> allowedOrigins) {
    }

    public record Auth(
            String jwtSecret,
            @DefaultValue("30") int accessTtlMinutes,
            @DefaultValue("14") int refreshTtlDays,
            /** 대리 로그인 세션 만료. tenant-plan.md FR-05 — 30분. */
            @DefaultValue("30") int impersonationTtlMinutes
    ) {
    }

    public record Ops(
            /** 운영 콘솔 접근 IP allowlist. 비어 있으면 제한하지 않는다(로컬 개발). */
            @DefaultValue("") List<String> ipAllowlist
    ) {
    }

    public record Llm(
            @DefaultValue("STUB") LlmProviderName defaultProvider,
            Credential google,
            Credential anthropic,
            Credential openai
    ) {
        public record Credential(@DefaultValue("") String apiKey) {
            public boolean configured() {
                return apiKey != null && !apiKey.isBlank();
            }
        }
    }

    public record Storage(
            // TODO(stub): S3 호환 저장소 미연동. 로컬 디스크로 대체.
            @DefaultValue("./var/uploads") String localPath
    ) {
    }

    public record Notification(
            // TODO(stub): 슬랙 미연동. 로그만 남긴다.
            @DefaultValue("") String slackWebhookUrl
    ) {
    }
}
