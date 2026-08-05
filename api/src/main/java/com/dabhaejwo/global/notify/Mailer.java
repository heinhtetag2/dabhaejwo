package com.dabhaejwo.global.notify;

/**
 * 메일 발송.
 *
 * <p>초대·OTP·비밀번호 재설정은 <b>메일이 실제로 나가야 완결되는 기능</b>이다.
 * 발송에 실패했는데 성공으로 응답하면 사용자는 오지 않는 메일을 기다린다 —
 * 그래서 실패는 예외로 올린다.
 */
public interface Mailer {

    /**
     * HTML 과 평문을 함께 보낸다(multipart/alternative).
     *
     * <p>평문을 같이 보내는 이유는 HTML 을 막는 클라이언트가 있기 때문이다.
     * 그때 본문이 비면 인증 코드를 못 받는 사람이 생긴다.
     */
    void send(String to, String subject, MailTemplate.Body body);

    /** 설정이 없으면 false. 이 경우 발송을 시도하지 않고 명시적으로 거절한다. */
    boolean available();
}
