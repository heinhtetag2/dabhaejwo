package com.dabhaejwo.domain.knowledge.entity;

import com.dabhaejwo.global.security.BotScope;
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

    /** 어느 서비스의 것인가. 조회는 전부 이 값으로 좁힌다. */
    @Column(name = "bot_id", nullable = false)
    private UUID botId;

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

    /** 오브젝트 저장소 안의 키. 웹페이지 문서는 원본 파일이 없으므로 null 이다. */
    @Column(name = "storage_key")
    private String storageKey;

    /** 서버가 확장자로 판정한 MIME. 클라이언트가 보낸 값을 그대로 적지 않는다. */
    @Column(name = "content_type")
    private String contentType;

    /** 사용자가 올린 원래 파일명. 화면 표시용이며 저장소 키로는 쓰지 않는다. */
    @Column(name = "original_filename")
    private String originalFilename;

    /** 같은 파일 재업로드 판별용. */
    @Column(name = "content_sha256")
    private String contentSha256;

    @Column(name = "indexed_at")
    private OffsetDateTime indexedAt;

    /**
     * 이 문서의 조각을 만든 공급사·모델.
     *
     * <p>nullable 이다 — 이 컬럼이 생기기 전에 학습된 문서는 출처를 모른다.
     * 모르는 것은 "지금 설정과 다르다"로 취급해 다시 학습 대상에 넣는다.
     */
    @Column(name = "embedding_provider")
    private String embeddingProvider;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected KnowledgeDocument() {
    }

    public static KnowledgeDocument of(BotScope scope,
                                       UUID sourceId,
                                       String title,
                                       String path,
                                       DocumentStatus status) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.tenantId = scope.tenantId();
        document.botId = scope.botId();
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

    /**
     * 업로드한 원본을 문서에 연결한다.
     *
     * <p>저장소에 올린 <b>뒤에</b> 부른다. 반대로 하면 DB 에는 키가 있는데 파일이 없는
     * 상태가 만들어지고, 나중에 읽으려다 실패한다.
     */
    public void attachFile(String storageKey, String contentType, String originalFilename,
                           long sizeBytes, String contentSha256) {
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.sizeBytes = sizeBytes;
        this.contentSha256 = contentSha256;
    }

    /**
     * 인덱싱을 시작했다. 워커가 집어간 문서를 다른 워커가 또 집어가지 않도록 표시한다.
     * 옛 실패 코드를 지운다 — 다시 시도하는 중인데 지난 오류가 화면에 남아 있으면 안 된다.
     */
    public void markProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.errorCode = null;
    }

    /**
     * 학습 완료. <b>무엇으로 임베딩했는지 함께 남긴다.</b>
     *
     * <p>이 기록이 없으면 공급사·모델을 바꿨을 때 무엇을 다시 학습해야 하는지 알 수 없어
     * 전부 지우고 처음부터 하는 수밖에 없다. 다른 모델이 만든 벡터끼리는 거리를 비교할 수 없다.
     */
    public void markIndexed(int chunks, String provider, String model) {
        this.status = DocumentStatus.INDEXED;
        this.chunkCount = chunks;
        this.errorCode = null;
        this.indexedAt = OffsetDateTime.now();
        this.embeddingProvider = provider;
        this.embeddingModel = model;
    }

    /**
     * 지금 설정으로 다시 학습해야 하는가.
     *
     * <p>출처를 모르는 문서(이 컬럼이 생기기 전에 학습된 것)는 <b>다르다고 본다</b> —
     * 해시 임베딩 시절 조각이라 실제로 다시 해야 한다.
     */
    public boolean staleEmbedding(String currentProvider, String currentModel) {
        if (status != DocumentStatus.INDEXED) {
            return false;
        }
        return !currentProvider.equals(embeddingProvider) || !currentModel.equals(embeddingModel);
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
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

    public UUID getBotId() {
        return botId;
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

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }
}
