package com.dabhaejwo.domain.knowledge.controller;

import com.dabhaejwo.domain.knowledge.dto.request.DocumentExcludeRequest;
import com.dabhaejwo.domain.knowledge.dto.request.SourceAutoRefreshRequest;
import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeDocumentResponse;
import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeSourceResponse;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.service.KnowledgeService;
import com.dabhaejwo.global.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/app/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
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
    public void retryFailed(@RequestParam(required = false) UUID sourceId) {
        knowledgeService.retryFailed(sourceId);
    }
}
