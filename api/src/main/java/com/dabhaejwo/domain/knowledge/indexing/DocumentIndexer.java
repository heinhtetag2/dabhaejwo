package com.dabhaejwo.domain.knowledge.indexing;

import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeChunkRepository;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.notification.service.NotificationEvents;
import com.dabhaejwo.global.llm.EmbedResult;
import com.dabhaejwo.global.llm.LlmGateway;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.UsagePurpose;
import com.dabhaejwo.global.storage.FileStorage;
import com.dabhaejwo.global.security.BotScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * 문서 하나를 학습 가능한 상태로 만든다.
 *
 * <p>원본 읽기 → 글자 뽑기 → 조각 내기 → 임베딩 → 저장. 어느 단계에서 실패하든
 * <b>문서를 FAILED 로 두고 이유를 남긴다.</b> 조용히 넘어가면 업체는 학습된 줄 알고 기다린다.
 *
 * <p>임베딩은 반드시 {@link LlmGateway} 를 지난다. 공급사를 직접 부르면 그 호출은
 * {@code ai_usage} 에 남지 않고, 원가 데이터는 사후에 복원할 수 없다.
 */
@Service
public class DocumentIndexer {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexer.class);

    /**
     * 한 번에 임베딩할 조각 수.
     *
     * <p>너무 크면 한 번 실패할 때 통째로 날아가고, 너무 작으면 왕복이 잦아진다.
     * 공급사 배치 상한(보통 100)보다 넉넉히 작게 잡는다.
     */
    private static final int EMBED_BATCH = 32;

    /** 문서 하나가 만들 수 있는 조각 수 상한. 20MB 문서 하나로 원가가 폭주하지 않게 막는다. */
    private static final int MAX_CHUNKS_PER_DOCUMENT = 400;

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final FileStorage fileStorage;
    private final TextExtractor textExtractor;
    private final LlmGateway llmGateway;
    private final EmbeddingSettings embeddingSettings;
    private final NotificationEvents notificationEvents;

    public DocumentIndexer(KnowledgeDocumentRepository documentRepository,
                           KnowledgeChunkRepository chunkRepository,
                           FileStorage fileStorage,
                           TextExtractor textExtractor,
                           LlmGateway llmGateway,
                           EmbeddingSettings embeddingSettings,
                           NotificationEvents notificationEvents) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.fileStorage = fileStorage;
        this.textExtractor = textExtractor;
        this.llmGateway = llmGateway;
        this.embeddingSettings = embeddingSettings;
        this.notificationEvents = notificationEvents;
    }

    /**
     * @return 성공하면 만들어진 조각 수, 실패하면 {@code -1}. 실패 이유는 문서에 기록된다
     */
    @Transactional
    public int index(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            // 인덱싱을 기다리는 사이 지워졌다. 오류가 아니다.
            return -1;
        }
        if (document.getStorageKey() == null) {
            return fail(document, "no_original_file");
        }

        document.markProcessing();

        try {
            TextExtractor.Extracted extracted = extract(document);
            if (extracted.failed()) {
                log.info("글자를 뽑지 못했습니다 — document={}, code={}", documentId, extracted.errorCode());
                return fail(document, extracted.errorCode());
            }

            List<String> chunks = Chunker.split(extracted.text());
            if (chunks.size() > MAX_CHUNKS_PER_DOCUMENT) {
                chunks = chunks.subList(0, MAX_CHUNKS_PER_DOCUMENT);
                // 조용히 자르지 않는다. 잘렸다는 사실이 어딘가에는 남아야 한다.
                log.warn("문서가 너무 길어 앞부분만 학습합니다 — document={}, 상한={}",
                        documentId, MAX_CHUNKS_PER_DOCUMENT);
            }

            LlmProviderName provider = embeddingProvider();
            String model = embeddingModel(provider);
            List<float[]> vectors = embedAll(new BotScope(document.getTenantId(), document.getBotId()), chunks, provider, model);
            chunkRepository.replaceForDocument(
                    new BotScope(document.getTenantId(), document.getBotId()),
                    documentId, chunks, vectors);
            // 무엇으로 학습했는지 함께 남긴다 — 공급사를 바꿨을 때 무엇을 다시 해야 하는지의 근거다.
            document.markIndexed(chunks.size(), provider.name(), model);

            log.info("학습 완료 — document={}, 조각={}", documentId, chunks.size());
            // 업로드하고 나면 언제 끝나는지 알 수 없다. 끝난 순간을 알려준다.
            notificationEvents.indexingDone(
                    new BotScope(document.getTenantId(), document.getBotId()),
                    document.getTitle(), chunks.size());
            return chunks.size();

        } catch (IOException e) {
            // 포맷이 깨졌거나 암호가 걸린 파일. 재시도해도 같은 결과다.
            log.info("파일을 해석하지 못했습니다 — document={}", documentId, e);
            return fail(document, "parse_failed");
        } catch (RuntimeException e) {
            // 저장소·공급사 오류처럼 다시 해보면 될 수도 있는 것들. 워커가 재시도한다.
            log.error("학습에 실패했습니다 — document={}", documentId, e);
            return fail(document, "indexing_failed");
        }
    }

    /**
     * 실패를 한 경로로 모은다. 상태만 바꾸고 알림을 빠뜨리면 업체는 학습된 줄 알고 기다린다 —
     * 실패 기록과 통보를 <b>같은 자리에서</b> 하는 편이 빠뜨릴 여지가 없다.
     */
    private int fail(KnowledgeDocument document, String errorCode) {
        document.markFailed(errorCode);
        notificationEvents.indexingFailed(
                new BotScope(document.getTenantId(), document.getBotId()),
                document.getTitle(), errorCode);
        return -1;
    }

    private TextExtractor.Extracted extract(KnowledgeDocument document) throws IOException {
        try (InputStream original = fileStorage.get(document.getStorageKey())) {
            String filename = document.getOriginalFilename() != null
                    ? document.getOriginalFilename() : document.getTitle();
            return textExtractor.extract(original, filename);
        }
    }

    /** 배치로 나눠 임베딩한다. 게이트웨이가 호출마다 {@code ai_usage} 를 적재한다. */
    private List<float[]> embedAll(BotScope scope, List<String> chunks,
                                   LlmProviderName provider, String model) {
        List<float[]> vectors = new java.util.ArrayList<>(chunks.size());

        for (int from = 0; from < chunks.size(); from += EMBED_BATCH) {
            List<String> batch = chunks.subList(from, Math.min(from + EMBED_BATCH, chunks.size()));
            EmbedResult result = llmGateway.embed(
                    scope, UsagePurpose.EMBED_DOC, provider, batch, model);

            if (result.vectors().size() != batch.size()) {
                throw new IllegalStateException(
                        "임베딩 개수가 요청과 다릅니다: " + result.vectors().size() + " != " + batch.size());
            }
            vectors.addAll(result.vectors());
        }
        return vectors;
    }

    /**
     * 쓸 임베딩 공급사·모델. {@link EmbeddingSettings} 한 곳에서만 정한다 —
     * 질문 검색도 같은 값을 봐야 하기 때문이다.
     */
    public LlmProviderName embeddingProvider() {
        return embeddingSettings.provider();
    }

    public String embeddingModel(LlmProviderName provider) {
        return embeddingSettings.model(provider);
    }

    /** 문서를 지울 때 조각도 함께 지운다. 남으면 없는 문서의 내용이 계속 검색된다. */
    @Transactional
    public void removeChunks(BotScope scope, UUID documentId) {
        chunkRepository.deleteByDocument(scope, documentId);
    }

    /** 다시 학습 대상으로 되돌린다. */
    @Transactional
    public void requeue(BotScope scope, UUID documentId) {
        documentRepository.findById(documentId)
                .filter(document -> document.getBotId().equals(scope.botId()))
                .ifPresent(document -> {
                    chunkRepository.deleteByDocument(scope, documentId);
                    document.requeue();
                });
    }

    /** 상태 판정에 쓰는 상수를 외부에 노출한다 — 워커가 같은 기준으로 조회한다. */
    public static DocumentStatus pendingStatus() {
        return DocumentStatus.PENDING;
    }
}
