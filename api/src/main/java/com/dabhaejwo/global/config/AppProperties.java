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
        Indexing indexing,
        Mail mail,
        Otp otp,
        Invite invite,
        Payment payment,
        // `notify` 는 Object.notify() 와 충돌해 record 컴포넌트명으로 쓸 수 없다.
        Notification notification,
        Cors cors,
        Security security
) {

    /**
     * 저장용 비밀을 감싸는 마스터 키.
     *
     * <p>공급사 API 키는 DB 에 암호화해 두지만 <b>이 키만은 환경변수에 남는다</b> —
     * 마스터 키까지 DB 에 있으면 자물쇠 옆에 열쇠를 두는 셈이다.
     *
     * <p>비어 있으면 자격증명 기능이 동작하지 않는다. 조용히 평문으로 떨어지지 않는다.
     */
    public record Security(@DefaultValue("") String encryptionKey) {
    }

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
            @DefaultValue("30") int impersonationTtlMinutes,
            /**
             * 리프레시 쿠키에 {@code Secure} 를 붙일지. 운영은 항상 true —
             * 없으면 평문 HTTP 로도 쿠키가 나가 중간에서 가로챌 수 있다.
             * 로컬은 http 라 false 로 둔다.
             */
            @DefaultValue("true") boolean cookieSecure,
            /**
             * 리프레시 쿠키의 {@code SameSite}.
             *
             * <p>기본 {@code Lax} 는 콘솔과 API 가 <b>같은 등록가능 도메인</b>일 때만 맞는다
             * (tagoplus.co.kr 아래 서브도메인, 또는 개발의 localhost). API 를 다른 도메인으로
             * 옮기면 {@code None} 으로 바꿔야 하고 그때는 {@code Secure} 가 필수다.
             */
            @DefaultValue("Lax") String cookieSameSite
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

    /**
     * 파일 저장소 (S3 호환 — Cloudflare R2).
     *
     * <p>비어 있으면 업로드를 명시적으로 거절한다. 로컬 디스크로 대체하지 않는다 —
     * 개발 PC 에서만 되는 기능을 만들면 실패를 배포 시점으로 미루는 것뿐이다.
     *
     * @param endpoint R2 계정 엔드포인트. {@code https://{accountId}.r2.cloudflarestorage.com}
     */
    public record Storage(
            @DefaultValue("") String bucket,
            @DefaultValue("") String endpoint,
            @DefaultValue("") String accessKeyId,
            @DefaultValue("") String secretAccessKey,
            /** 업로드 파일당 최대 크기(MB). tenant-plan.md §4.5 는 20MB 로 정했다. */
            @DefaultValue("20") int maxFileSizeMb
    ) {
        public boolean configured() {
            return !bucket.isBlank() && !endpoint.isBlank()
                    && !accessKeyId.isBlank() && !secretAccessKey.isBlank();
        }

        public long maxFileSizeBytes() {
            return (long) maxFileSizeMb * 1024 * 1024;
        }
    }

    /**
     * 학습 워커의 주기.
     *
     * <p>{@code @Scheduled} 는 바인딩된 record 가 아니라 프로퍼티 플레이스홀더만 읽으므로
     * 워커 쪽에도 같은 키가 문자열로 적혀 있다. 기본값을 양쪽에 두면 어긋나므로
     * <b>기본값은 여기에만 둔다</b> — 워커의 플레이스홀더에는 기본값이 없다.
     */
    public record Indexing(
            @DefaultValue("10000") long pollIntervalMs,
            @DefaultValue("15000") long initialDelayMs
    ) {
    }

    /**
     * 메일. <b>자격증명은 여기 없다</b> — spring.mail.* 이 환경변수로 받는다.
     * 여기는 발송자 표시와 링크가 가리킬 주소만 둔다.
     *
     * @param appBaseUrl 업체 대시보드. 초대·재설정 링크가 여기로 간다
     * @param opsBaseUrl 운영 콘솔. 운영자용 링크가 여기로 간다
     */
    public record Mail(
            @DefaultValue("") String from,
            @DefaultValue("답해줘") String fromName,
            @DefaultValue("http://localhost:4312") String appBaseUrl,
            @DefaultValue("http://localhost:4311") String opsBaseUrl
    ) {
        /** 보내는 주소가 없으면 발송할 수 없다. 호스트·계정은 Spring 이 검사한다. */
        public boolean configured() {
            return from != null && !from.isBlank();
        }
    }

    /**
     * 로그인 2단계 인증.
     *
     * <p>값을 코드에 두지 않는 이유는 운영에서 조정해야 하기 때문이다 —
     * 메일이 늦게 오는 환경에서는 만료를 늘려야 하고, 남용이 보이면 재발송을 줄여야 한다.
     */
    public record Otp(
            @DefaultValue("5") int ttlMinutes,
            @DefaultValue("5") int maxAttempts,
            @DefaultValue("10") int resendPerHour,
            /**
             * <b>개발 전용 우회.</b> 켜면 형식만 맞는 6자리 숫자면 무엇이든 통과한다.
             *
             * <p>메일을 기다리지 않고 화면을 오가며 개발하기 위한 것이다. 메일은 <b>그대로 나간다</b> —
             * 발송이 깨진 것을 이 플래그가 가려버리면 안 된다.
             *
             * <p>기본값은 false 이고, {@code production} 프로파일에서 true 면
             * {@code DevOtpGuard} 가 <b>기동을 막는다.</b> 설정 실수 하나로 2단계 인증이
             * 통째로 무의미해지는 것을 배포 시점에 발견하게 만든다.
             */
            @DefaultValue("false") boolean devAcceptAnyCode
    ) {
    }

    public record Invite(
            @DefaultValue("168") int ttlHours,
            @DefaultValue("24") int tempPasswordTtlHours
    ) {
    }

    /**
     * 결제 대행사(토스페이먼츠).
     *
     * <p>클라이언트 키는 여기 없다 — 브라우저 번들에 들어가는 값이라 프론트 환경변수로 간다.
     * 여기 있는 것은 <b>서버 밖으로 나가면 안 되는 것들</b>뿐이다.
     *
     * @param securityKey 웹훅 서명 검증용. 토스가 보낸 알림인지 확인한다
     */
    public record Payment(
            @DefaultValue("") String secretKey,
            @DefaultValue("") String securityKey
    ) {
        public boolean configured() {
            return secretKey != null && !secretKey.isBlank();
        }

        /** 라이브 키인가. 로그에 "테스트 키로 붙었다"를 남겨 운영 사고를 줄인다. */
        public boolean live() {
            return configured() && !secretKey.startsWith("test_");
        }
    }

    public record Notification(
            // TODO(stub): 슬랙 미연동. 로그만 남긴다.
            @DefaultValue("") String slackWebhookUrl
    ) {
    }
}
