package com.dabhaejwo.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 개발용 OTP 우회가 운영에 새어 들어가지 못하게 막는다.
 *
 * <p>{@code dabhaejwo.otp.dev-accept-any-code} 가 켜져 있으면 아무 6자리 숫자로도 로그인된다 —
 * 2단계 인증이 통째로 없는 것과 같다. 환경변수 하나가 운영에 따라가는 사고는 실제로 잦으므로,
 * <b>{@code production} 프로파일에서는 기동 자체를 실패시킨다.</b>
 *
 * <p>조용히 꺼주지 않는 이유는 그러면 아무도 설정이 잘못됐다는 사실을 모르기 때문이다.
 */
@Configuration
public class DevOtpGuard {

    private static final Logger log = LoggerFactory.getLogger(DevOtpGuard.class);

    /**
     * 메서드 이름이 클래스 이름과 <b>달라야 한다.</b>
     *
     * <p>{@code @Configuration} 클래스 자체가 {@code devOtpGuard} 라는 이름으로 등록되므로,
     * {@code @Bean} 메서드까지 같은 이름이면 한 이름에 두 번 등록되어 기동이 실패한다
     * ({@code A bean with that name has already been defined}). 실제로 그렇게 났었다.
     */
    @Bean
    ApplicationListener<ApplicationReadyEvent> devOtpGuardListener(AppProperties properties,
                                                                   Environment environment) {
        return event -> {
            if (!properties.otp().devAcceptAnyCode()) {
                return;
            }
            boolean production = environment.matchesProfiles("production");
            if (production) {
                throw new IllegalStateException(
                        "dabhaejwo.otp.dev-accept-any-code 가 켜진 채로 production 프로파일이 떴습니다. "
                                + "이 설정은 인증 코드를 검사하지 않습니다 — 끄고 다시 배포하세요");
            }
            log.warn("=".repeat(78));
            log.warn("[개발 우회] 로그인 인증 코드를 검사하지 않습니다. 6자리 숫자면 무엇이든 통과합니다.");
            log.warn("[개발 우회] 메일은 그대로 발송됩니다. 운영 배포 전 OTP_DEV_ACCEPT_ANY 를 지우세요.");
            log.warn("=".repeat(78));
        };
    }
}
