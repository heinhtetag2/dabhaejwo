package com.dabhaejwo.domain.knowledge.indexing;

import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.global.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 학습 대기 문서를 집어 처리한다.
 *
 * <p>왜 업로드 요청 안에서 하지 않는가. 20MB PDF 의 글자를 뽑고 조각마다 임베딩을 부르면
 * 수십 초가 걸린다. 그 시간 동안 요청이 묶이면 브라우저는 타임아웃으로 끊고, 사용자는
 * 업로드가 실패한 줄 안다 — 실제로는 파일이 올라가 있는데도.
 *
 * <p><b>단일 인스턴스를 전제한다.</b> 여러 대로 늘리면 같은 문서를 둘이 집어갈 수 있다.
 * 지금은 상태를 PROCESSING 으로 먼저 바꿔 창을 좁혔을 뿐 잠금이 아니다 —
 * 확장 시 `SELECT ... FOR UPDATE SKIP LOCKED` 나 jobs 테이블 기반 큐로 바꿔야 한다
 * (docs/IMPROVEMENTS.md).
 */
@Component
public class IndexingWorker {

    private static final Logger log = LoggerFactory.getLogger(IndexingWorker.class);

    /** 한 번에 처리할 문서 수. 너무 크면 한 주기가 길어지고 그동안 새 문서가 밀린다. */
    private static final int BATCH = 5;

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentIndexer indexer;

    public IndexingWorker(KnowledgeDocumentRepository documentRepository,
                          DocumentIndexer indexer,
                          AppProperties properties) {
        this.documentRepository = documentRepository;
        this.indexer = indexer;
        // 문서가 처리되지 않을 때 가장 먼저 확인할 것이 "워커가 도는가"다. 기동 로그에 남긴다.
        log.info("학습 워커 — {}ms 마다 최대 {}건, 첫 실행은 {}ms 뒤",
                properties.indexing().pollIntervalMs(), BATCH, properties.indexing().initialDelayMs());
    }

    /**
     * 고정 지연 10초. 업로드 직후 곧 처리되면서도, 대기열이 비었을 때 DB 를 계속 두드리지 않는다.
     *
     * <p>{@code fixedDelay} 다 — 이전 실행이 끝난 뒤부터 센다. {@code fixedRate} 로 두면
     * 처리가 10초를 넘길 때 실행이 겹쳐 같은 문서를 둘이 잡는다.
     */
    @Scheduled(fixedDelayString = "${dabhaejwo.indexing.poll-interval-ms}",
            initialDelayString = "${dabhaejwo.indexing.initial-delay-ms}")
    public void run() {
        List<KnowledgeDocument> waiting = documentRepository
                .findAllByStatusOrderByCreatedAtAsc(DocumentStatus.PENDING, Limit.of(BATCH));
        if (waiting.isEmpty()) {
            return;
        }

        log.info("학습 대기 문서 {}건을 처리합니다", waiting.size());
        for (KnowledgeDocument document : waiting) {
            try {
                indexer.index(document.getId());
            } catch (RuntimeException e) {
                // 한 문서가 터져도 나머지는 계속 처리한다. 실패 기록은 indexer 가 남긴다.
                log.error("문서 처리 중 예기치 못한 오류 — document={}", document.getId(), e);
            }
        }
    }
}
