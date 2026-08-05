package com.dabhaejwo.domain.knowledge.service;

import com.dabhaejwo.domain.knowledge.dto.response.KnowledgeSearchResponse;
import com.dabhaejwo.domain.knowledge.indexing.EmbeddingSettings;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeChunkRepository;
import com.dabhaejwo.domain.pricing.entity.ModelPrice;
import com.dabhaejwo.domain.pricing.repository.ModelPriceRepository;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.llm.EmbedResult;
import com.dabhaejwo.global.llm.LlmGateway;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.UsagePurpose;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 질문과 가까운 학습 조각 찾기.
 *
 * <p>답변 생성의 앞단이자, 업체가 "왜 이렇게 답했지"를 확인하는 창이기도 하다.
 *
 * <p>질문 임베딩도 {@link LlmGateway} 를 지난다 — 문서 임베딩만 원가에 잡고 질문을
 * 빼면 질문이 많은 업체일수록 원가가 실제보다 싸 보인다. 목적이 {@code EMBED_QUERY} 로
 * 갈리므로 나중에 둘을 나눠 볼 수 있다.
 */
@Service
public class KnowledgeSearchService {

    /** 기본 조각 수. 답변에 넣을 근거는 이 정도면 충분하고, 더 넣으면 원가만 늘어난다. */
    private static final int DEFAULT_LIMIT = 5;

    /** 상한. 검색으로 남의 문서를 통째로 긁어가는 통로가 되지 않게 막는다. */
    private static final int MAX_LIMIT = 20;

    /** 질문 길이 상한. 이보다 길면 임베딩 비용만 늘고 검색 품질은 오히려 나빠진다. */
    private static final int MAX_QUERY_CHARS = 500;

    private final KnowledgeChunkRepository chunkRepository;
    private final LlmGateway llmGateway;
    private final EmbeddingSettings embeddingSettings;

    public KnowledgeSearchService(KnowledgeChunkRepository chunkRepository,
                                  LlmGateway llmGateway,
                                  EmbeddingSettings embeddingSettings) {
        this.chunkRepository = chunkRepository;
        this.llmGateway = llmGateway;
        this.embeddingSettings = embeddingSettings;
    }

    @Transactional
    public KnowledgeSearchResponse search(String query, Integer limit) {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return search(tenantId, query, limit);
    }

    /**
     * 답변 파이프라인이 쓰는 형태. 화면용 DTO 가 아니라 조각 자체를 돌려준다 —
     * 프롬프트에는 본문이 필요하고, 유사도는 답변 실패 판정에 쓰인다.
     */
    @Transactional
    public List<KnowledgeChunkRepository.Match> searchEvidence(UUID tenantId, String query, int limit) {
        return searchInternal(tenantId, query, limit);
    }

    /**
     * 테넌트를 직접 받는 형태. 방문자에게는 {@code CurrentAuth.tenantUser()} 가 없다.
     */
    @Transactional
    public KnowledgeSearchResponse search(UUID tenantId, String query, Integer limit) {
        String trimmed = normalize(query);
        return KnowledgeSearchResponse.of(trimmed,
                searchInternal(tenantId, trimmed, limit == null ? DEFAULT_LIMIT : limit));
    }

    private List<KnowledgeChunkRepository.Match> searchInternal(UUID tenantId, String query, int limit) {
        String trimmed = normalize(query);
        int size = Math.clamp(limit, 1, MAX_LIMIT);

        // 문서를 학습할 때와 같은 공급사·모델을 쓴다. 다르면 벡터끼리 거리가 아무 의미도 없어
        // 검색이 조용히 아무것도 못 찾는다 — 오류도 로그도 없이 "답변 실패"로만 보인다.
        LlmProviderName provider = embeddingSettings.provider();
        EmbedResult embedded = llmGateway.embed(
                tenantId, UsagePurpose.EMBED_QUERY, provider, List.of(trimmed),
                embeddingSettings.model(provider));
        if (embedded.vectors().isEmpty()) {
            throw new BusinessException(ErrorCode.FEATURE_NOT_READY,
                    "질문을 해석하지 못했습니다. 잠시 후 다시 시도해 주세요");
        }
        return chunkRepository.search(tenantId, embedded.vectors().getFirst(), size);
    }

    private String normalize(String query) {
        String trimmed = query == null ? "" : query.strip();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "검색할 질문을 입력해 주세요");
        }
        return trimmed.length() > MAX_QUERY_CHARS ? trimmed.substring(0, MAX_QUERY_CHARS) : trimmed;
    }

}
