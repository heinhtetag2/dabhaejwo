package com.dabhaejwo.domain.knowledge.dto.response;

import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 학습 문서. api-contracts.md §9-2.
 *
 * @param errorCode 운영자용 원문 코드({@code pdf_parse_timeout}). 한글 설명은 프론트가 매핑한다 —
 *                  코드를 서버에서 문장으로 바꾸면 화면마다 표현이 갈리고 번역도 서버에 묶인다
 */
public record KnowledgeDocumentResponse(
        UUID id,
        UUID sourceId,
        String title,
        String path,
        DocumentStatus status,
        String errorCode,
        int chunkCount,
        Long sizeBytes,
        OffsetDateTime indexedAt,
        /** 업로드한 원본이 있는 문서만 값이 있다. 화면은 이 값으로 삭제 버튼 노출을 정한다. */
        String storageKey,
        String originalFilename) {

    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getSourceId(),
                document.getTitle(),
                document.getPath(),
                document.getStatus(),
                document.getErrorCode(),
                document.getChunkCount(),
                document.getSizeBytes(),
                document.getIndexedAt(),
                document.getStorageKey(),
                document.getOriginalFilename());
    }
}
