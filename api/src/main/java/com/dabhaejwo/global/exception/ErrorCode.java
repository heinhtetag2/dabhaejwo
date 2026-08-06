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
    /**
     * OTP 가 틀렸거나, 만료됐거나, 이미 썼거나, 시도 횟수를 넘겼다.
     * <b>넷을 구분해 알려주지 않는다</b> — 어느 쪽인지 알면 유효한 챌린지를 골라낼 수 있다.
     */
    OTP_INVALID(HttpStatus.UNAUTHORIZED, "인증 코드가 올바르지 않거나 만료되었습니다"),
    /** 임시 비밀번호로 들어왔다. 비밀번호를 바꾸기 전에는 다른 요청을 받지 않는다. */
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "비밀번호를 새로 설정해야 합니다"),
    INVITE_INVALID(HttpStatus.NOT_FOUND, "초대 링크가 올바르지 않거나 만료되었습니다"),
    /** 정지·해지된 업체의 위젯. 키가 살아 있어도 답하지 않는다 — 안 내는 사이트에서 원가가 계속 나간다. */
    TENANT_INACTIVE(HttpStatus.FORBIDDEN, "현재 이용할 수 없는 업체입니다"),

    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이미지를 찾을 수 없습니다"),
    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 업체를 찾을 수 없습니다"),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 요금제를 찾을 수 없습니다"),
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공통 질문을 찾을 수 없습니다"),
    OPERATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 운영자를 찾을 수 없습니다"),
    MODEL_PRICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 모델의 단가가 등록되어 있지 않습니다"),
    IMPERSONATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대리 접속 세션을 찾을 수 없습니다"),
    SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 지식 소스를 찾을 수 없습니다"),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 문서를 찾을 수 없습니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 팀원을 찾을 수 없습니다"),
    LEAD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 연락처를 찾을 수 없습니다"),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 대화를 찾을 수 없습니다"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 메시지를 찾을 수 없습니다"),
    ANSWER_GAP_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 질문을 찾을 수 없습니다"),
    FLAG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 기능 플래그를 찾을 수 없습니다"),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 문의를 찾을 수 없습니다"),
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 작업을 찾을 수 없습니다"),
    CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 공급사 키가 없습니다"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 알림을 찾을 수 없습니다"),

    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 상태 변경입니다"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "다른 사용자가 먼저 수정했습니다"),

    QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "이번 달 한도를 초과했습니다"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 잦습니다"),

    COST_CAP_REACHED(HttpStatus.SERVICE_UNAVAILABLE, "일일 원가 상한에 도달했습니다"),
    /** 마스터 키 미설정. 조용히 평문으로 떨어지지 않고 거부한다. */
    ENCRYPTION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "암호화 키가 설정되지 않았습니다"),
    /** 마스터 키가 바뀌었거나 암호문이 조작됐다. 어느 쪽이든 그 값을 쓰면 안 된다. */
    CREDENTIAL_UNREADABLE(HttpStatus.SERVICE_UNAVAILABLE, "저장된 자격증명을 읽을 수 없습니다"),
    /**
     * 외부 의존성이 아직 연결되지 않았다. 상태만 바꾸고 아무 일도 일어나지 않으면
     * 사용자는 처리된 줄 알고 기다린다 — 조용한 성공 처리 금지 (workflow-rules).
     */
    FEATURE_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE, "아직 연결되지 않은 기능입니다"),
    LLM_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 공급사에 연결할 수 없습니다"),
    /** 공급사 안전 정책으로 응답이 막혔다. 우리 잘못도 장애도 아니므로 따로 구분한다. */
    LLM_RESPONSE_BLOCKED(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답이 안전 정책으로 차단되었습니다"),
    /**
     * 메일 발송 실패. <b>조용히 넘기지 않는다</b> — 초대·비밀번호 재설정은 메일이 나가야
     * 완결되는 기능이라, 성공으로 응답하면 사용자가 오지 않는 메일을 기다린다.
     */
    MAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "메일을 보내지 못했습니다"),
    /**
     * 결제 실패. <b>조용히 넘기지 않는다</b> — 성공으로 응답하면 업체는 유료 전환이 끝난 줄 알고,
     * 우리는 받지 않은 돈을 받은 것으로 기록한다.
     */
    PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "결제를 처리하지 못했습니다"),
    /** 등록된 결제수단이 없다. 카드 등록 화면으로 보낸다. */
    BILLING_KEY_MISSING(HttpStatus.BAD_REQUEST, "등록된 결제수단이 없습니다");

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
