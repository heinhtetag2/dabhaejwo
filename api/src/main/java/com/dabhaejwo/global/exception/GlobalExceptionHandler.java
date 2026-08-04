package com.dabhaejwo.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        // 비즈니스 예외는 정상적인 흐름의 일부다. 스택트레이스를 남기지 않는다.
        log.info("business error: {} - {}", e.errorCode(), e.getMessage());
        return ResponseEntity.status(e.errorCode().status())
                .body(ErrorResponse.of(e.errorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, detail));
    }

    /**
     * 본문을 읽지 못한 경우 — 깨진 JSON, 잘못된 인코딩, 타입 불일치.
     *
     * <p>이것도 <b>클라이언트 입력 오류다.</b> 500 으로 내보내면 서버 장애처럼 보여
     * 실제 장애와 구분이 안 되고, 호출한 쪽은 무엇을 고쳐야 할지 알 수 없다.
     *
     * <p>파서의 원문 메시지는 내부 구조를 드러내므로 응답에 싣지 않는다(security-rules).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.info("unreadable request body: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED,
                        "요청 본문을 읽지 못했습니다. JSON 형식과 UTF-8 인코딩을 확인하세요"));
    }

    /**
     * 업로드 크기 초과.
     *
     * <p>톰캣이 서비스 레이어에 닿기 전에 잘라내므로 우리 검증 메시지가 나가지 못한다.
     * 여기서 받지 않으면 <b>500</b> 이 되는데, 파일이 큰 것은 서버 장애가 아니라
     * 사용자가 고칠 수 있는 입력 문제다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException e) {
        log.info("업로드 크기 초과: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED,
                        "파일이 너무 큽니다. 20MB 까지 올릴 수 있습니다"));
    }

    /**
     * 예상하지 못한 예외. 원인은 로그에만 남기고 응답에는 내부 구조를 노출하지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "일시적인 오류가 발생했습니다"));
    }
}
