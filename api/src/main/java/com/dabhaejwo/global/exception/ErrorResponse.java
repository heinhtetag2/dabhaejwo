package com.dabhaejwo.global.exception;

/**
 * 에러 응답. api-contracts.md §0-3 의 형태와 정확히 일치한다.
 * 스택트레이스·내부 구조를 절대 담지 않는다 (security-rules).
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.message());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}
