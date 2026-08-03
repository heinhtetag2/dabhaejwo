package com.dabhaejwo.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 단일 계열. 도메인별 예외 클래스를 만들지 않는다.
 * code 는 기계용, message 는 사람용 — docs/architecture/api-contracts.md §0-3 과 일치시킬 것.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    REASON_REQUIRED(HttpStatus.BAD_REQUEST, "사유를 입력해야 합니다"),

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    IMPERSONATION_EXPIRED(HttpStatus.UNAUTHORIZED, "대리 접속 세션이 만료되었습니다"),

    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    IMPERSONATION_FORBIDDEN_ACTION(HttpStatus.FORBIDDEN, "대리 접속 중에는 할 수 없는 작업입니다"),
    ORIGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "등록되지 않은 주소에서의 요청입니다"),

    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 업체를 찾을 수 없습니다"),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 요금제를 찾을 수 없습니다"),
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공통 질문을 찾을 수 없습니다"),
    OPERATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 운영자를 찾을 수 없습니다"),
    MODEL_PRICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 모델의 단가가 등록되어 있지 않습니다"),
    IMPERSONATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대리 접속 세션을 찾을 수 없습니다"),

    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 상태 변경입니다"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "다른 사용자가 먼저 수정했습니다"),

    QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "이번 달 한도를 초과했습니다"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 잦습니다"),

    COST_CAP_REACHED(HttpStatus.SERVICE_UNAVAILABLE, "일일 원가 상한에 도달했습니다"),
    LLM_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 공급사에 연결할 수 없습니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
