package com.dabhaejwo.domain.knowledge.repository;

import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    /**
     * 요금제 한도와 비교할 문서 수. 업체가 제외(EXCLUDED)한 문서는 학습 대상이 아니므로 세지 않는다.
     */
    @Query("""
            SELECT COUNT(d) FROM KnowledgeDocument d
            WHERE d.tenantId = :tenantId AND d.status <> com.dabhaejwo.domain.knowledge.entity.DocumentStatus.EXCLUDED
            """)
    long countActive(@Param("tenantId") UUID tenantId);

    /** 홈의 지식 상태 막대 — 상태별 건수를 한 번에 가져온다. */
    @Query("""
            SELECT d.status AS status, COUNT(d) AS count FROM KnowledgeDocument d
            WHERE d.tenantId = :tenantId GROUP BY d.status
            """)
    List<StatusCount> countByStatus(@Param("tenantId") UUID tenantId);

    Page<KnowledgeDocument> findAllByTenantIdAndSourceIdOrderByCreatedAtDesc(
            UUID tenantId, UUID sourceId, Pageable pageable);

    long countByTenantIdAndSourceId(UUID tenantId, UUID sourceId);

    long countByTenantIdAndStatus(UUID tenantId, DocumentStatus status);

    interface StatusCount {
        DocumentStatus getStatus();

        long getCount();
    }
}
