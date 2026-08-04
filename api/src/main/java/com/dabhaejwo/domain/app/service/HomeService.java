package com.dabhaejwo.domain.app.service;

import com.dabhaejwo.domain.app.dto.response.HomeSummaryResponse;
import com.dabhaejwo.domain.conversation.repository.ConversationRepository;
import com.dabhaejwo.domain.conversation.repository.MessageRepository;
import com.dabhaejwo.domain.gap.entity.GapStatus;
import com.dabhaejwo.domain.gap.repository.AnswerGapRepository;
import com.dabhaejwo.domain.knowledge.entity.DocumentStatus;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.dabhaejwo.domain.lead.repository.LeadRepository;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 홈 요약.
 *
 * <p>모든 질의에 테넌트 조건이 붙는다. 이 화면은 업체가 자기 데이터만 보는 곳이며
 * 타 업체 숫자가 한 칸이라도 섞이면 P0 다.
 */
@Service
public class HomeService {

    /** 많이 물어본 질문 — 프로토타입과 같은 5개. */
    private static final int TOP_QUESTION_LIMIT = 5;
    private static final int TOP_QUESTION_DAYS = 7;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AnswerGapRepository gapRepository;
    private final LeadRepository leadRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public HomeService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       AnswerGapRepository gapRepository,
                       LeadRepository leadRepository,
                       KnowledgeDocumentRepository documentRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.gapRepository = gapRepository;
        this.leadRepository = leadRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public HomeSummaryResponse summary() {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();
        UUID tenantId = user.tenantId();

        OffsetDateTime todayStart = startOfDay(LocalDate.now());
        OffsetDateTime tomorrowStart = todayStart.plusDays(1);
        OffsetDateTime yesterdayStart = todayStart.minusDays(1);
        OffsetDateTime weekStart = todayStart.minusDays(7);
        OffsetDateTime twoWeeksStart = todayStart.minusDays(14);

        long todayConv = conversationRepository
                .countByTenantIdAndStartedAtBetween(tenantId, todayStart, tomorrowStart);
        long yesterdayConv = conversationRepository
                .countByTenantIdAndStartedAtBetween(tenantId, yesterdayStart, todayStart);

        return new HomeSummaryResponse(
                todayConv,
                todayConv - yesterdayConv,
                successPercent(tenantId, todayStart, tomorrowStart),
                successPercent(tenantId, twoWeeksStart, weekStart),
                gapRepository.countByTenantIdAndStatus(tenantId, GapStatus.OPEN),
                leadRepository.countByTenantIdAndCreatedAtBetween(tenantId, todayStart, tomorrowStart),
                leadRepository.countByTenantIdAndCreatedAtBetween(tenantId, weekStart, tomorrowStart),
                averageLatencyMs(tenantId, weekStart),
                knowledge(tenantId),
                topQuestions(tenantId, todayStart.minusDays(TOP_QUESTION_DAYS)));
    }

    /**
     * 답변 성공률. 저장 답변(FAQ)도 성공으로 센다 — 방문자 입장에서는 답을 받은 것이다.
     *
     * @return 대화가 한 건도 없으면 null. 0% 로 내리면 "다 실패했다"로 읽힌다
     */
    private Integer successPercent(UUID tenantId, OffsetDateTime from, OffsetDateTime to) {
        long total = messageRepository.countBotMessages(tenantId, from, to, false);
        if (total == 0) {
            return null;
        }
        long answered = messageRepository.countBotMessages(tenantId, from, to, true);
        return Math.toIntExact(Math.round(answered * 100.0 / total));
    }

    /**
     * TODO(stub): 답변 파이프라인이 없어 {@code messages.latency_ms} 가 비어 있다.
     * 그동안은 null 이 내려가고 화면은 "—" 를 보여준다. 그럴듯한 값을 지어내지 않는다.
     */
    private Integer averageLatencyMs(UUID tenantId, OffsetDateTime from) {
        Double average = messageRepository.averageLatency(tenantId, from);
        return average == null ? null : Math.toIntExact(Math.round(average));
    }

    private HomeSummaryResponse.Knowledge knowledge(UUID tenantId) {
        Map<DocumentStatus, Long> counts = new EnumMap<>(DocumentStatus.class);
        for (KnowledgeDocumentRepository.StatusCount row : documentRepository.countByStatus(tenantId)) {
            counts.put(row.getStatus(), row.getCount());
        }
        long indexed = counts.getOrDefault(DocumentStatus.INDEXED, 0L);
        long failed = counts.getOrDefault(DocumentStatus.FAILED, 0L);
        // PENDING 과 PROCESSING 은 업체 눈에 똑같이 "처리 중"이다. 내부 구분을 화면에 흘리지 않는다.
        long processing = counts.getOrDefault(DocumentStatus.PENDING, 0L)
                + counts.getOrDefault(DocumentStatus.PROCESSING, 0L);
        // EXCLUDED 는 업체가 뺀 것이라 실패가 아니고 한도에도 안 잡힌다.
        return new HomeSummaryResponse.Knowledge(indexed + processing + failed, indexed, processing, failed);
    }

    private List<HomeSummaryResponse.TopQuestion> topQuestions(UUID tenantId, OffsetDateTime from) {
        return messageRepository
                .findTopQuestions(tenantId, from, PageRequest.of(0, TOP_QUESTION_LIMIT))
                .stream()
                .map(row -> new HomeSummaryResponse.TopQuestion(row.getQuestion(), row.getAskCount()))
                .toList();
    }

    /** 하루 경계는 UTC 로 잡는다. 시각을 전부 timestamptz 로 저장하므로 기준을 하나로 둔다. */
    private OffsetDateTime startOfDay(LocalDate day) {
        return day.atStartOfDay().atOffset(ZoneOffset.UTC);
    }
}
