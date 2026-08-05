package com.dabhaejwo.global.notify;

import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * SMTP 발송.
 *
 * <p>HTML 과 평문을 함께 보낸다(multipart/alternative). 사용자 입력이 본문에 들어가므로
 * HTML 쪽은 {@link MailTemplate} 이 반드시 이스케이프한다.
 *
 * <p>발송 실패는 삼키지 않는다. 대신 <b>원인은 로그에만 남기고</b> 사용자에게는
 * 일반 메시지를 준다 — SMTP 오류 문구에 서버 주소나 계정이 섞여 나온다.
 */
public class SmtpMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);

    private final JavaMailSender sender;
    private final AppProperties.Mail config;

    public SmtpMailer(JavaMailSender sender, AppProperties.Mail config) {
        this.sender = sender;
        this.config = config;
    }

    @Override
    public void send(String to, String subject, MailTemplate.Body body) {
        try {
            MimeMessage message = sender.createMimeMessage();
            // multipart=true 여야 평문과 HTML 을 함께 실을 수 있다.
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            // 평문을 먼저 준다 — 순서가 뒤바뀌면 일부 클라이언트가 평문을 우선 표시한다.
            helper.setText(body.text(), body.html());
            helper.setFrom(from());
            sender.send(message);
            log.info("메일을 보냈습니다 — to={}, subject={}", to, subject);

        } catch (MailException | jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            log.error("메일 발송 실패 — to={}, subject={}", to, subject, e);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED,
                    "메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요");
        }
    }

    private InternetAddress from() throws UnsupportedEncodingException {
        return new InternetAddress(config.from(), config.fromName(), StandardCharsets.UTF_8.name());
    }

    @Override
    public boolean available() {
        return true;
    }
}
