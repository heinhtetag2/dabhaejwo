package com.dabhaejwo.domain.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 학습 대상 문서 하나. 웹페이지 한 장이거나 업로드한 파일 하나다.
 *
 * <p>임베딩 자체는 {@code knowledge_chunks} 에 있고 이 엔티티는 그걸 모른다 —
 * Hibernate 가 pgvector 타입을 매핑하지 못하므로 벡터는 native query 로만 다룬다.
 */
@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false)
    private String title;

    /** 웹페이지 경로 또는 파일명. */
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    /** 운영자용 원문 코드(`pdf_parse_timeout` 등). 한글 설명은 프론트가 매핑한다. */
    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "indexed_at")
    private OffsetDateTime indexedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected KnowledgeDocument() {
    }

    public static KnowledgeDocument of(UUID tenantId,
                                       UUID sourceId,
                                       String title,
                                       String path,
                                       DocumentStatus status) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.tenantId = tenantId;
        document.sourceId = sourceId;
        document.title = title;
        document.path = path;
        document.status = status;
        document.createdAt = OffsetDateTime.now();
        if (status == DocumentStatus.INDEXED) {
            document.indexedAt = document.createdAt;
        }
        return document;
    }

    public void markIndexed(int chunks) {
        this.status = DocumentStatus.INDEXED;
        this.chunkCount = chunks;
        this.errorCode = null;
        this.indexedAt = OffsetDateTime.now();
    }

    public void markFailed(String code) {
        this.status = DocumentStatus.FAILED;
        this.errorCode = code;
    }

    /** 다시 학습 대기열로. 실패 코드를 남겨두면 화면에 옛 오류가 계속 보인다. */
    public void requeue() {
        this.status = DocumentStatus.PENDING;
        this.errorCode = null;
    }

    /**
     * 학습에서 제외한다. <b>삭제가 아니다</b> — 목록에는 남고 학습 대상에서만 빠지며
     * 요금제 한도에도 잡히지 않는다. 채용 공고나 이용약관처럼 챗봇이 답할 필요 없는
     * 페이지를 빼는 데 쓴다 (tenant-plan.md §4.5).
     */
    public void exclude() {
        this.status = DocumentStatus.EXCLUDED;
        this.errorCode = null;
        this.chunkCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public OffsetDateTime getIndexedAt() {
        return indexedAt;
    }
}
