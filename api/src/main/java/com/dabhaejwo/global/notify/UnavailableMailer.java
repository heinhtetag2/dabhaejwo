package com.dabhaejwo.global.notify;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMTP 미설정 시 쓰이는 구현.
 *
 * <p><b>조용히 성공시키지 않는다.</b> 메일이 안 나가는데 초대가 성공했다고 응답하면
 * 초대한 사람은 상대가 링크를 못 받은 사실을 영영 모른다. 로그로 남기고 거절한다.
 */
public class UnavailableMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(UnavailableMailer.class);

    @Override
    public void send(String to, String subject, MailTemplate.Body body) {
        log.error("메일 발송이 요청됐으나 SMTP 가 설정되지 않았습니다 — to={}, subject={}", to, subject);
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "메일 발송이 설정되지 않았습니다. 관리자에게 문의해 주세요");
    }

    @Override
    public boolean available() {
        return false;
    }
}
