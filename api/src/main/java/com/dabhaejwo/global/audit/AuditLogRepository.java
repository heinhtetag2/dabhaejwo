package com.dabhaejwo.global.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 조회와 적재만 있다. 수정·삭제 메서드를 만들지 않는다 —
 * JpaRepository 가 delete 를 물려주지만 호출하면 DB 트리거가 막는다.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
