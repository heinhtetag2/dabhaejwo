package com.dabhaejwo.domain.knowledge.controller;

import com.dabhaejwo.domain.knowledge.dto.request.DocumentExcludeRequest;
import com.dabhaejwo.domain.knowledge.dto.request.SourceAutoRefreshRequest;
import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeDocumentResponse;
import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeSearchResponse;
import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeSourceResponse;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.service.DocumentUploadService;
import com.dabhaejwo.domain.knowledge.service.KnowledgeSearchService;
import com.dabhaejwo.domain.knowledge.service.KnowledgeService;
import com.dabhaejwo.global.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/app/bots/{botId}/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final DocumentUploadService uploadService;
    private final KnowledgeSearchService searchService;

    public KnowledgeController(KnowledgeService knowledgeService,
                               DocumentUploadService uploadService,
                               KnowledgeSearchService searchService) {
        this.knowledgeService = knowledgeService;
        this.uploadService = uploadService;
        this.searchService = searchService;
    }

    @GetMapping("/sources")
    public List<KnowledgeSourceResponse> sources() {
        return knowledgeService.listSources();
    }

    @PatchMapping("/sources/{id}")
    public KnowledgeSourceResponse changeAutoRefresh(@PathVariable UUID id,
                                                     @Valid @RequestBody SourceAutoRefreshRequest request) {
        return knowledgeService.changeAutoRefresh(id, request.autoRefresh());
    }

    @PostMapping("/sources/{id}/recrawl")
    public void recrawl(@PathVariable UUID id) {
        knowledgeService.recrawl(id);
    }

    @GetMapping("/documents")
    public PageResponse<KnowledgeDocumentResponse> documents(
            @RequestParam(required = false) UUID sourceId,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return knowledgeService.listDocuments(sourceId, status, q, page, size);
    }

    @PatchMapping("/documents/{id}")
    public KnowledgeDocumentResponse changeExcluded(@PathVariable UUID id,
                                                    @Valid @RequestBody DocumentExcludeRequest request) {
        return knowledgeService.changeExcluded(id, request.excluded());
    }

    @PostMapping("/documents/retry-failed")
    public RetryResult retryFailed(@RequestParam(required = false) UUID sourceId) {
        return new RetryResult(knowledgeService.retryFailed(sourceId));
    }

    /**
     * 질문과 가까운 학습 조각. 답변을 만들지 않고 근거만 보여준다 —
     * "챗봇이 이 질문에 뭘 보고 답하는지"를 업체가 직접 확인하는 창이다.
     */
    @GetMapping("/search")
    public KnowledgeSearchResponse search(@RequestParam String q,
                                          @RequestParam(required = false) Integer limit) {
        return searchService.search(q, limit);
    }

    /**
     * 파일 업로드. 원본은 오브젝트 저장소에, 문서 행은 DB 에 남는다.
     *
     * <p>응답 시점에는 문서가 {@code PENDING} 이다 — 글자 뽑기와 임베딩은 워커가
     * 뒤에서 처리한다. 화면이 그 상태를 그대로 보여준다.
     */
    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeDocumentResponse upload(@RequestPart("file") MultipartFile file) {
        return uploadService.upload(file);
    }

    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id) {
        uploadService.delete(id);
    }

    /** @param requeued 다시 학습 대기로 되돌린 문서 수 */
    public record RetryResult(int requeued) {
    }
}
