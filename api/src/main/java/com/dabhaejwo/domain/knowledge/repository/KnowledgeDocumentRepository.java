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
    long countActiveAcrossBots(@Param("tenantId") UUID tenantId);

    /** 홈의 지식 상태 막대 — 상태별 건수를 한 번에 가져온다. */
    @Query("""
            SELECT d.status AS status, COUNT(d) AS count FROM KnowledgeDocument d
            WHERE d.botId = :botId GROUP BY d.status
            """)
    List<StatusCount> countByStatus(@Param("botId") UUID botId);

    /**
     * 문서 목록. 소스·상태는 선택이고, 테넌트 조건만 항상 붙는다.
     *
     * <p>제목과 경로를 함께 훑는다 — 업체는 "/guide/delivery" 로도 찾고 "배송" 으로도 찾는다.
     *
     * <p>{@code q} 는 <b>null 을 받지 않는다.</b> 검색어가 없으면 빈 문자열을 넘긴다.
     * 이 파라미터는 함수 안에만 나와서 Hibernate 가 타입을 추론할 근거가 없고,
     * null 을 주면 PostgreSQL 이 {@code bytea} 로 바인딩해 {@code lower(bytea) does not exist} 로 터진다.
     * {@code LIKE '%%'} 는 전부 매칭이라 필터가 없는 것과 같다.
     */
    @Query("""
            SELECT d FROM KnowledgeDocument d
            WHERE d.botId = :botId
              AND (:sourceId IS NULL OR d.sourceId = :sourceId)
              AND (:status IS NULL OR d.status = :status)
              AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(d.path, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY d.createdAt DESC
            """)
    Page<KnowledgeDocument> search(@Param("botId") UUID botId,
                                   @Param("sourceId") UUID sourceId,
                                   @Param("status") DocumentStatus status,
                                   @Param("q") String q,
                                   Pageable pageable);

    /** 소스별 문서 수. 목록에서 N+1 이 되지 않게 한 번에 가져온다. */
    @Query("""
            SELECT d.sourceId AS sourceId, COUNT(d) AS count FROM KnowledgeDocument d
            WHERE d.botId = :botId GROUP BY d.sourceId
            """)
    List<SourceCount> countBySource(@Param("botId") UUID botId);

    List<KnowledgeDocument> findAllByBotIdAndStatus(UUID botId, DocumentStatus status);

    /**
     * 학습 대기 문서. 워커가 오래 기다린 것부터 집는다 —
     * 늦게 올린 파일이 먼저 처리되면 먼저 올린 업체가 계속 밀린다.
     */
    List<KnowledgeDocument> findAllByStatusOrderByCreatedAtAsc(
            DocumentStatus status, org.springframework.data.domain.Limit limit);

    /** 같은 파일을 또 올렸는지. 해시가 같으면 내용이 같다. */
    java.util.Optional<KnowledgeDocument> findFirstByBotIdAndContentSha256(
            UUID botId, String contentSha256);

    long countByBotIdAndStatus(UUID botId, DocumentStatus status);

    /** 전 업체 합계. 운영자가 "지금 학습이 몇 건 밀렸나"를 볼 때 쓴다. */
    long countByStatus(DocumentStatus status);

    interface SourceCount {
        UUID getSourceId();

        long getCount();
    }

    interface StatusCount {
        DocumentStatus getStatus();

        long getCount();
    }
}
