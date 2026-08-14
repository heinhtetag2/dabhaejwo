package com.dabhaejwo.domain.bot.service;

import com.dabhaejwo.domain.bot.entity.Bot;
import com.dabhaejwo.domain.bot.entity.BotStatus;
import com.dabhaejwo.domain.bot.repository.BotRepository;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.entity.KnowledgeDocument;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.global.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 삭제 예약된 서비스를 유예 기간 뒤에 실제로 지운다.
 *
 * <p><b>이것이 이 시스템 최초의 purge 배치다.</b> {@code tenants.purge_after} 는 해지 시
 * 채워지지만 읽는 코드가 없었다 — "해지 30일 뒤 자료를 파기한다"는 약속이 코드에 없었다는 뜻이다.
 * 업체 해지 purge 는 아직 여기 없다({@code docs/IMPROVEMENTS.md}) — {@code audit_logs} 가
 * FK 로 업체 행을 붙들고 있어 별도 결정이 필요하다.
 *
 * <p><b>순서가 전부다.</b> R2 오브젝트 → 조각 → 문서 → 나머지 → {@code bots} 행.
 * 반대로 하면 저장소에 고아 파일이 남고 <b>그것을 찾을 키가 사라진다</b> —
 * 키는 {@code knowledge_documents.storage_key} 에만 있다.
 *
 * <p>저장소 삭제가 실패하면 그 서비스는 <b>그대로 둔다.</b> DB 만 지우고 넘어가면 파일이
 * 영원히 남고 아무도 그 사실을 모른다. 다음 주기에 다시 시도한다.
 */
@Component
public class BotPurgeWorker {

    private static final Logger log = LoggerFactory.getLogger(BotPurgeWorker.class);

    /** 한 번에 처리할 서비스 수. 저장소 왕복이 길어 한 주기에 다 하려 들지 않는다. */
    private static final int BATCH = 20;

    private final BotRepository botRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final FileStorage fileStorage;
    private final JdbcTemplate jdbc;

    public BotPurgeWorker(BotRepository botRepository,
                          KnowledgeDocumentRepository documentRepository,
                          FileStorage fileStorage,
                          JdbcTemplate jdbc) {
        this.botRepository = botRepository;
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
        this.jdbc = jdbc;
    }

    /**
     * 매일 새벽 3시(한국 시간).
     *
     * <p>자동 청구(4시)보다 앞에 둔다 — 지워진 서비스가 그날 청구 계산에 끼지 않게 한다.
     *
     * <p>주기를 설정으로 뺀 이유는 <b>하루에 한 번만 도는 코드는 검증할 수 없기 때문이다.</b>
     * 실제로 파기가 일어나는지 보려면 3시까지 기다리거나 서버 시계를 돌려야 했다.
     * 기본값은 운영 값 그대로이므로 아무것도 설정하지 않으면 새벽 3시다.
     */
    @Scheduled(cron = "${app.bot-purge-cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void run() {
        purgeDue(OffsetDateTime.now());
    }

    /**
     * 유예가 끝난 것부터 지운다.
     *
     * <p>서비스마다 <b>독립된 트랜잭션</b>이다 — 한 곳이 터져도 나머지는 지워진다.
     * 한 번에 묶으면 마지막 실패가 앞선 삭제까지 되감는다.
     */
    public int purgeDue(OffsetDateTime now) {
        List<Bot> due = botRepository.findAllByStatus(BotStatus.DELETING).stream()
                .filter(bot -> bot.purgeDue(now))
                .limit(BATCH)
                .toList();
        int done = 0;
        for (Bot bot : due) {
            try {
                purge(bot.getId());
                done++;
            } catch (RuntimeException e) {
                // 다음 주기에 다시 시도한다. 조용히 넘기지 않는다 — 파일이 남았을 수 있다.
                log.error("서비스 파기에 실패했습니다 — bot={}", bot.getId(), e);
            }
        }
        if (done > 0) {
            log.info("서비스 {}개를 파기했습니다", done);
        }
        return done;
    }

    /**
     * 서비스 하나를 지운다.
     *
     * <p>저장소를 <b>DB 보다 먼저</b> 지운다. 키가 DB 에만 있기 때문이다.
     */
    @Transactional
    public void purge(java.util.UUID botId) {
        Bot bot = botRepository.findById(botId).orElse(null);
        if (bot == null || bot.getStatus() != BotStatus.DELETING) {
            return;
        }

        /*
         * 1) 저장소 오브젝트.
         *
         * **상태를 가리지 않는다** — 학습에 실패한 문서에도 파일은 올라가 있다.
         * 실패하면 예외가 올라가 트랜잭션이 되감기고 다음 주기에 다시 온다.
         */
        for (DocumentStatus status : DocumentStatus.values()) {
            for (KnowledgeDocument document : documentRepository.findAllByBotIdAndStatus(botId, status)) {
                if (document.getStorageKey() != null) {
                    fileStorage.delete(document.getStorageKey());
                }
            }
        }

        // 2) DB. 자식부터 — FK 가 걸린 순서 그대로다.
        jdbc.update("DELETE FROM messages WHERE conversation_id IN "
                + "(SELECT id FROM conversations WHERE bot_id = ?)", botId);
        for (String table : List.of("message_feedback", "leads", "conversations", "answer_gaps",
                "faqs", "knowledge_chunks", "knowledge_documents", "knowledge_sources",
                "allowed_origins", "bot_settings", "bot_daily_usage", "jobs")) {
            jdbc.update("DELETE FROM " + table + " WHERE bot_id = ?", botId);
        }

        /*
         * `ai_usage` 는 지우지 않는다 — 원가 원장이다. FK 가 없으므로 서비스 행이 사라져도
         * bot_id 값은 남고, 그것이 가리키는 서비스가 없다는 것이 사실이다.
         * `audit_logs` 도 그대로다 — 3년 불변이고, 그 기록은 업체를 가리킨다.
         */
        botRepository.delete(bot);
        log.info("서비스를 파기했습니다 — bot={}, name={}", botId, bot.getName());
    }
}
