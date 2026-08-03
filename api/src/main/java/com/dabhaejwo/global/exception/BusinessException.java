package com.dabhaejwo.global.exception;

/** 이 서비스의 단일 예외 계열. 도메인별 예외 클래스를 파생시키지 않는다. */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황을 구체적으로 알려야 할 때만 사용한다. */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
