package com.dabhaejwo.global.audit;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
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
}
