package com.dabhaejwo.domain.knowledge.service;

import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeDocumentResponse;
import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeSourceResponse;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeSource;
import com.dabhaejwo.domain.knowledge.indexing.DocumentIndexer;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeSourceRepository;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import com.dabhaejwo.domain.bot.service.BotContext;
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
    private final BotContext botContext;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentIndexer indexer;

    public KnowledgeService(KnowledgeSourceRepository sourceRepository,
                            KnowledgeDocumentRepository documentRepository,
                            DocumentIndexer indexer,
                            BotContext botContext) {
        this.sourceRepository = sourceRepository;
        this.documentRepository = documentRepository;
        this.indexer = indexer;
        this.botContext = botContext;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeSourceResponse> listSources() {
        UUID botId = botContext.scope().botId();

        Map<UUID, Long> counts = new HashMap<>();
        for (KnowledgeDocumentRepository.SourceCount row : documentRepository.countBySource(botId)) {
            counts.put(row.getSourceId(), row.getCount());
        }
        return sourceRepository.findAllByBotIdOrderByCreatedAtAsc(botId).stream()
                .map(source -> KnowledgeSourceResponse.of(source, counts.getOrDefault(source.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeDocumentResponse> listDocuments(UUID sourceId,
                                                                 DocumentStatus status,
                                                                 String q,
                                                                 int page,
                                                                 Integer size) {
        UUID botId = botContext.scope().botId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size));
        // 빈 문자열이 "필터 없음"이다 — 위 쿼리 주석 참조.
        String query = (q == null) ? "" : q.strip();

        return PageResponse.of(
                documentRepository.search(botId, sourceId, status, query, pageable),
                KnowledgeDocumentResponse::from);
    }

    @Transactional
    public KnowledgeSourceResponse changeAutoRefresh(UUID sourceId, boolean autoRefresh) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        KnowledgeSource source = sourceRepository.findByIdAndBotId(sourceId, botContext.scope().botId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_NOT_FOUND));
        source.changeAutoRefresh(autoRefresh);

        long count = documentRepository.countBySource(botContext.scope().botId()).stream()
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
        KnowledgeDocument document = find(documentId, botContext.scope().botId());

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
        sourceRepository.findByIdAndBotId(sourceId, botContext.scope().botId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_NOT_FOUND));

        log.warn("recrawl 요청을 받았으나 크롤러가 연결되어 있지 않습니다 (tenant={}, source={})",
                botContext.scope().botId(), sourceId);
        throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                "사이트 다시 읽기는 아직 연결되지 않았습니다. 준비되면 안내드리겠습니다");
    }

    /**
     * 실패 문서 다시 학습.
     *
     * <p>{@code PENDING} 으로 되돌리기만 한다 — 실제 처리는 워커가 한다. 여기서 바로
     * 돌리면 실패 문서가 100건일 때 요청 하나가 몇 분을 잡는다.
     *
     * <p>되돌릴 게 없으면 <b>조용히 성공시키지 않는다.</b> "다시 학습을 눌렀는데 아무 일도
     * 없었다"는 상태를 업체가 알 수 있어야 한다.
     */
    @Transactional
    public int retryFailed(UUID sourceId) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();

        List<KnowledgeDocument> targets = relearnTargets(botContext.scope().botId(), sourceId);

        if (targets.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "다시 학습할 문서가 없습니다");
        }
        for (KnowledgeDocument document : targets) {
            indexer.requeue(botContext.scope(), document.getId());
        }

        log.info("{}건을 다시 학습 대기로 되돌렸습니다 (bot={})", targets.size(), botContext.scope().botId());
        return targets.size();
    }

    /**
     * 다시 학습해야 하는 문서.
     *
     * <p>실패한 것만이 아니다. <b>운영팀이 임베딩 공급사를 바꾸면 이미 학습된 문서도
     * 대상이 된다</b> — 다른 모델이 만든 벡터끼리는 거리를 비교할 수 없어, 그대로 두면
     * 검색이 조용히 엉뚱한 결과를 낸다. 실패보다 알아채기 어려운 고장이다.
     *
     * <p>공급사 설정을 읽지 못하면(단가 미등록 등) 실패분만 대상으로 삼는다 —
     * 설정 문제로 업체의 "다시 학습"이 통째로 막히면 안 된다.
     */
    private List<KnowledgeDocument> relearnTargets(UUID botId, UUID sourceId) {
        List<KnowledgeDocument> failed =
                documentRepository.findAllByBotIdAndStatus(botId, DocumentStatus.FAILED).stream()
                        .filter(document -> sourceId == null || sourceId.equals(document.getSourceId()))
                        .toList();

        List<KnowledgeDocument> stale;
        try {
            LlmProviderName provider = indexer.embeddingProvider();
            String model = indexer.embeddingModel(provider);
            stale = documentRepository
                    .findAllByBotIdAndStatus(botId, DocumentStatus.INDEXED).stream()
                    .filter(document -> sourceId == null || sourceId.equals(document.getSourceId()))
                    .filter(document -> document.staleEmbedding(provider.name(), model))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("임베딩 설정을 읽지 못해 실패분만 다시 학습합니다 (bot={})", botId, e);
            stale = List.of();
        }

        List<KnowledgeDocument> targets = new java.util.ArrayList<>(failed);
        targets.addAll(stale);
        return targets;
    }

    private KnowledgeDocument find(UUID documentId, UUID botId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        // 서비스로 좁힌다 — 업체로만 좁히면 옆 서비스의 문서가 열린다.
        if (!document.getBotId().equals(botId)) {
            // 남의 문서다. 존재 여부를 알려주지 않기 위해 없는 것과 같은 응답을 낸다.
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }
}
