package com.dabhaejwo.global.audit;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.BotScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 감사 기록의 유일한 적재 경로. 운영자의 쓰기 액션은 같은 커밋에서 여기를 호출한다 —
 * "나중에 추가"는 반드시 누락된다 (core security-rules).
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 사유가 필요한 행위인데 사유가 비어 있으면 거부한다.
     * 공백만 입력한 경우도 거부한다 — tenant-plan.md §6.1.
     *
     * <p>별도 트랜잭션으로 남긴다. 뒤따르는 비즈니스 트랜잭션이 롤백되더라도
     * "그 시점에 이 운영자가 이 데이터에 접근하려 했다"는 사실은 지워지면 안 된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID operatorId,
                       AuditAction action,
                       UUID tenantId,
                       String reason,
                       Map<String, Object> meta) {
        String normalized = reason == null ? "" : reason.strip();
        if (action.reasonRequired() && normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.REASON_REQUIRED);
        }
        repository.save(AuditLog.of(operatorId, action, tenantId, normalized, meta));
    }

    public void record(UUID operatorId, AuditAction action, UUID tenantId, String reason) {
        record(operatorId, action, tenantId, reason, Map.of());
    }

    /**
     * 대리 접속 중인 운영자가 고객 대화를 열었다.
     *
     * <p>사유는 대리 접속을 시작할 때 이미 받았다. 대화를 열 때마다 다시 묻지 않고
     * 그 세션을 가리키는 사실을 기록한다 — 세션 원문 사유는 {@code impersonation_sessions}
     * 에 있고 감사 기록 화면에서 이어 볼 수 있다.
     *
     * <p><b>어느 서비스의 대화였는지 함께 남긴다.</b> 업체가 서비스를 여럿 운영하면
     * "이 업체를 봤다"만으로는 무엇을 봤는지 알 수 없다 — 쇼핑몰 상담과 채용 문의는
     * 민감도가 다르다. 이 테이블은 3년 보존·수정 불가라 <b>나중에 채울 수 없다.</b>
     *
     * <p>스키마를 건드리지 않는다. {@code meta} jsonb 가 정확히 이 용도로 있다.
     *
     * <p>TODO(T7): 세션 엔티티가 생기면 원문 사유를 함께 적재해 조인 없이 읽히게 한다.
     */
    public void recordImpersonatedRead(UUID operatorId,
                                       BotScope scope,
                                       UUID sessionId,
                                       UUID conversationId) {
        record(operatorId, AuditAction.VIEW_CONVERSATIONS, scope.tenantId(),
                "대리 접속 세션 중 대화 열람",
                Map.of("impersonationSessionId", String.valueOf(sessionId),
                        "botId", String.valueOf(scope.botId()),
                        "conversationId", String.valueOf(conversationId)));
    }
}
