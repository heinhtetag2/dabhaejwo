package com.dabhaejwo.global.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 조회와 적재만 있다. 수정·삭제 메서드를 만들지 않는다 —
 * JpaRepository 가 delete 를 물려주지만 호출하면 DB 트리거가 막는다.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 업체 활동 이력에 합쳐 넣을 감사 항목. 이력은 화면에서 다시 잘리므로 상한을 둔다. */
    List<AuditLog> findTop100ByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * 감사 기록 화면의 검색. 업체·운영자·행위는 선택이고, 기간은 <b>항상 받는다.</b>
     *
     * <p>기간에 null 을 허용하지 않는 이유는 두 가지다. ① 3년치가 쌓인 뒤 전 구간을 훑으면
     * 느리다 — 호출부가 기본 범위를 정하게 강제한다. ② {@code :from IS NULL} 형태로 두면
     * PostgreSQL 이 그 파라미터의 타입을 추론하지 못해
     * {@code could not determine data type of parameter} 로 터진다.
     * uuid·enum 은 비교 대상 컬럼에서 타입이 잡히지만 timestamptz 는 그렇지 않다.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
              AND (:operatorId IS NULL OR a.operatorId = :operatorId)
              AND (:action IS NULL OR a.action = :action)
              AND a.createdAt >= :from
              AND a.createdAt < :to
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(@Param("tenantId") UUID tenantId,
                          @Param("operatorId") UUID operatorId,
                          @Param("action") AuditAction action,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to,
                          Pageable pageable);
}
