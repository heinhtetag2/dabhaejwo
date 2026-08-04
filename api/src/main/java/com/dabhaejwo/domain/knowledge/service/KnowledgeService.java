package com.dabhaejwo.domain.knowledge.service;

import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeDocumentResponse;
import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeSourceResponse;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeSource;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeSourceRepository;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 지식 소스와 학습 문서.
 *
 * <p>업체에게는 "크롤링"·"임베딩"·"청크" 라는 말을 쓰지 않는다 — 사이트 다시 읽기, 학습,
 * (노출하지 않음)이다 (tenant-plan.md §1.3). 그 규칙은 화면 문구에서 지키고 여기서는
 * 내부 이름을 쓴다.
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeSourceRepository sourceRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public KnowledgeService(KnowledgeSourceRepository sourceRepository,
                            KnowledgeDocumentRepository documentRepository) {
        this.sourceRepository = sourceRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeSourceResponse> listSources() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();

        Map<UUID, Long> counts = new HashMap<>();
        for (KnowledgeDocumentRepository.SourceCount row : documentRepository.countBySource(tenantId)) {
            counts.put(row.getSourceId(), row.getCount());
        }
        return sourceRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(source -> KnowledgeSourceResponse.of(source, counts.getOrDefault(source.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeDocumentResponse> listDocuments(UUID sourceId,
                                                                 DocumentStatus status,
                                                                 String q,
                                                                 int page,
                                                                 Integer size) {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size));
        // 빈 문자열이 "필터 없음"이다 — 위 쿼리 주석 참조.
        String query = (q == null) ? "" : q.strip();

        return PageResponse.of(
                documentRepository.search(tenantId, sourceId, status, query, pageable),
                KnowledgeDocumentResponse::from);
    }

    @Transactional
    public KnowledgeSourceResponse changeAutoRefresh(UUID sourceId, boolean autoRefresh) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        KnowledgeSource source = sourceRepository.findByIdAndTenantId(sourceId, user.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_NOT_FOUND));
        source.changeAutoRefresh(autoRefresh);

        long count = documentRepository.countBySource(user.tenantId()).stream()
                .filter(row -> row.getSourceId().equals(sourceId))
                .mapToLong(KnowledgeDocumentRepository.SourceCount::getCount)
                .findFirst()
                .orElse(0L);
        return KnowledgeSourceResponse.of(source, count);
    }

    /**
     * 문서 제외·복구. 제외는 삭제가 아니다 — 목록에는 남고 학습 대상에서만 빠진다.
     * 요금제 한도에도 잡히지 않는다.
     */
    @Transactional
    public KnowledgeDocumentResponse changeExcluded(UUID documentId, boolean excluded) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        KnowledgeDocument document = find(documentId, user.tenantId());

        if (excluded) {
            document.exclude();
        } else {
            document.requeue();
        }
        return KnowledgeDocumentResponse.from(document);
    }

    /**
     * 사이트 다시 읽기.
     *
     * <p>TODO(stub): 크롤러가 아직 없다. 상태만 바꾸고 아무 일도 일어나지 않으면 업체는
     * 학습된 줄 알고 기다리므로, 조용히 성공시키지 않고 명시적으로 거절한다.
     */
    @Transactional(readOnly = true)
    public void recrawl(UUID sourceId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        sourceRepository.findByIdAndTenantId(sourceId, user.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_NOT_FOUND));

        log.warn("recrawl 요청을 받았으나 크롤러가 연결되어 있지 않습니다 (tenant={}, source={})",
                user.tenantId(), sourceId);
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "사이트 다시 읽기는 아직 연결되지 않았습니다. 준비되면 안내드리겠습니다");
    }

    /**
     * 실패 문서 다시 학습.
     *
     * <p>TODO(stub): 임베딩 워커가 아직 없다. {@link #recrawl} 과 같은 이유로 거절한다.
     */
    @Transactional(readOnly = true)
    public void retryFailed(UUID sourceId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        long failed = documentRepository.countByTenantIdAndStatus(user.tenantId(), DocumentStatus.FAILED);

        log.warn("다시 학습 요청을 받았으나 임베딩 워커가 연결되어 있지 않습니다 (tenant={}, failed={})",
                user.tenantId(), failed);
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "다시 학습은 아직 연결되지 않았습니다. 준비되면 안내드리겠습니다");
    }

    private KnowledgeDocument find(UUID documentId, UUID tenantId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        if (!document.getTenantId().equals(tenantId)) {
            // 남의 문서다. 존재 여부를 알려주지 않기 위해 없는 것과 같은 응답을 낸다.
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }
}
