package com.dabhaejwo.global.config;

import com.dabhaejwo.global.notify.Mailer;
import com.dabhaejwo.global.notify.SmtpMailer;
import com.dabhaejwo.global.notify.UnavailableMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 메일 발송기 선택.
 *
 * <p>설정이 없으면 {@link UnavailableMailer} 다 — <b>로컬 로그 출력으로 대체하지 않는다.</b>
 * 개발에서만 되는 척하면 "메일이 안 온다"는 사실이 배포 시점으로 미뤄진다.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    Mailer mailer(JavaMailSender sender, AppProperties properties) {
        AppProperties.Mail config = properties.mail();
        if (!config.configured()) {
            log.warn("SMTP 가 설정되지 않았습니다 — 초대·OTP·비밀번호 재설정 메일이 나가지 않습니다");
            return new UnavailableMailer();
        }
        log.info("SMTP 발송 준비됨 — from={}", config.from());
        return new SmtpMailer(sender, config);
    }
}
