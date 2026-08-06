package com.dabhaejwo.domain.chat.service;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.repository.BotSettingsRepository;
import com.dabhaejwo.domain.chat.dto.response.AnswerResponse;
import com.dabhaejwo.domain.conversation.entity.Message;
import com.dabhaejwo.domain.conversation.repository.MessageRepository;
import com.dabhaejwo.domain.gap.entity.AnswerGap;
import com.dabhaejwo.domain.gap.entity.GapReason;
import com.dabhaejwo.domain.gap.repository.AnswerGapRepository;
import com.dabhaejwo.domain.guard.entity.CostGuard;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeChunkRepository;
import com.dabhaejwo.domain.knowledge.service.KnowledgeSearchService;
import com.dabhaejwo.domain.plan.entity.PlanModelAssignment;
import com.dabhaejwo.domain.plan.repository.PlanModelAssignmentRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.llm.GenerateRequest;
import com.dabhaejwo.global.llm.GenerateResult;
import com.dabhaejwo.global.llm.LlmGateway;
import com.dabhaejwo.global.llm.UsagePurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 방문자 질문 하나에 답한다.
 *
 * <p>순서는 <b>근거를 먼저 찾고, 근거가 있을 때만 모델을 부른다</b>. 근거 없이 모델을 부르면
 * 두 가지가 동시에 나빠진다 — 모르는 것을 지어내고, 지어낸 답에 돈까지 나간다.
 *
 * <p>답변에 실패한 질문은 {@code answer_gaps} 로 올라간다. 이게 제품의 개선 루프다 —
 * 못 답한 질문이 쌓이고, 업체가 그걸 보고 공통 질문으로 만들면 다음부터는 모델도 안 거친다.
 */
@Service
public class AnswerService {

    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    /**
     * 답변 출력 토큰 상한. 원가를 예측 가능하게 만든다.
     * 위젯 말풍선에 들어갈 분량이라 길 이유가 없다.
     */
    private static final int MAX_OUTPUT_TOKENS = 700;

    private final KnowledgeSearchService searchService;
    private final BotSettingsRepository botSettingsRepository;
    private final PlanModelAssignmentRepository assignmentRepository;
    private final TenantRepository tenantRepository;
    private final MessageRepository messageRepository;
    private final AnswerGapRepository gapRepository;
    private final LlmGateway llmGateway;

    public AnswerService(KnowledgeSearchService searchService,
                         BotSettingsRepository botSettingsRepository,
                         PlanModelAssignmentRepository assignmentRepository,
                         TenantRepository tenantRepository,
                         MessageRepository messageRepository,
                         AnswerGapRepository gapRepository,
                         LlmGateway llmGateway) {
        this.searchService = searchService;
        this.botSettingsRepository = botSettingsRepository;
        this.assignmentRepository = assignmentRepository;
        this.tenantRepository = tenantRepository;
        this.messageRepository = messageRepository;
        this.gapRepository = gapRepository;
        this.llmGateway = llmGateway;
    }

    /**
     * @param conversationId 대화. {@code null} 이면 대시보드 미리보기다 — 기록을 남기지 않는다
     * @param path           방문자가 있던 페이지. 답변 개선 목록에서 "어느 페이지에서 물었나"로 쓰인다
     */
    @Transactional
    public AnswerResponse answer(UUID tenantId, UUID conversationId, String question,
                                 String path, CostGuard guard) {
        String trimmed = question == null ? "" : question.strip();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "질문을 입력해 주세요");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
        BotSettings settings = botSettingsRepository.findById(tenantId)
                .orElseGet(() -> BotSettings.defaults(tenantId, tenant.getName()));
        PlanModelAssignment assignment = assignment(tenant);

        OffsetDateTime askedAt = OffsetDateTime.now();
        long startedAt = System.nanoTime();
        List<KnowledgeChunkRepository.Match> evidence =
                searchService.searchEvidence(tenantId, trimmed, chunkCount(assignment, guard));

        double best = evidence.isEmpty() ? 0.0 : evidence.getFirst().similarity();
        double threshold = guard.getAnswerFailSimilarity().doubleValue();

        if (evidence.isEmpty() || best < threshold) {
            // 근거가 없다. 모델을 부르지 않는다 — 부르면 지어낸 답에 돈까지 나간다.
            log.info("근거를 찾지 못해 답하지 않습니다 — tenant={}, 최고유사도={}, 기준={}",
                    tenantId, String.format("%.4f", best), threshold);
            return fail(tenantId, conversationId, trimmed, path, settings, askedAt, elapsedMs(startedAt));
        }

        GenerateResult result = llmGateway.generate(tenantId, UsagePurpose.ANSWER,
                assignment.getProvider(),
                new GenerateRequest(
                        assignment.getModel(),
                        PromptBuilder.systemPrompt(settings, guard.getCommonPrompt()),
                        PromptBuilder.userPrompt(trimmed, evidence),
                        MAX_OUTPUT_TOKENS),
                conversationId);

        String answer = truncate(result.text(), guard.getAnswerMaxLength());
        int latencyMs = elapsedMs(startedAt);

        UUID messageId = record(tenantId, conversationId, trimmed, answer, true,
                askedAt, latencyMs, evidence);
        // 자유 질문에는 후속을 붙이지 않는다 — 무엇을 물었는지에 대응하는 후속을
        // 우리가 고를 근거가 없다. 업체가 지정한 것은 공통 질문에만 달려 있다.
        return new AnswerResponse(true, false, answer, links(evidence), messageId, List.of());
    }

    /**
     * 답변 실패. 업체가 정한 안내 문구를 그대로 내보내고 질문을 개선 목록에 올린다.
     *
     * <p>여기서 아무 말이나 지어내면 방문자는 틀린 답을 받고, 업체는 놓친 질문이 있다는
     * 사실조차 모른다. <b>못 답한 것을 못 답했다고 하는 것이 이 제품의 핵심 기능이다.</b>
     */
    private AnswerResponse fail(UUID tenantId, UUID conversationId, String question,
                                String path, BotSettings settings,
                                OffsetDateTime askedAt, int latencyMs) {
        String message = settings.getFallbackMessage();
        UUID messageId = record(tenantId, conversationId, question, message, false,
                askedAt, latencyMs, List.of());

        // 미리보기(대화 없음)는 실제 방문자가 물은 게 아니므로 개선 목록을 오염시키지 않는다.
        if (conversationId != null) {
            gapRepository.findByTenantIdAndQuestionNorm(tenantId, AnswerGap.normalize(question))
                    .ifPresentOrElse(
                            gap -> gap.recur(question, path, message),
                            () -> gapRepository.save(AnswerGap.of(
                                    tenantId, question, GapReason.ANSWER_FAILED, path, message)));
        }
        return new AnswerResponse(false, false, message, List.of(), messageId, List.of());
    }

    /** @return 저장한 봇 메시지 id. 미리보기면 {@code null} */
    private UUID record(UUID tenantId, UUID conversationId, String question, String answer,
                        boolean answered, OffsetDateTime askedAt, int latencyMs,
                        List<KnowledgeChunkRepository.Match> evidence) {
        if (conversationId == null) {
            return null;
        }
        messageRepository.save(Message.fromVisitor(tenantId, conversationId, question, askedAt));

        // 답변 시각은 질문 시각 + 걸린 시간이다. 둘 다 now() 로 찍으면 동률이 되어 순서가 뒤집힌다.
        Message bot = Message.fromBot(tenantId, conversationId, answer, answered, false, null,
                askedAt.plusNanos(Math.max(latencyMs, 1) * 1_000_000L));
        bot.measured(latencyMs);
        messageRepository.save(bot);
        messageRepository.flush();

        // 근거 문서는 "왜 이렇게 답했나"의 유일한 기록이다. 지금 안 남기면 나중에 못 만든다.
        String sourceIds = evidence.stream()
                .map(KnowledgeChunkRepository.Match::documentId)
                .distinct()
                .map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        if (!evidence.isEmpty()) {
            messageRepository.attachSourceDocuments(bot.getId(), sourceIds);
        }
        return bot.getId();
    }

    /**
     * 프롬프트에 실을 조각 수. 요금제 배정이 우선이고, 없으면 운영 기본값을 쓴다.
     * 조각이 많을수록 정확하지만 입력 토큰이 비례해 늘어난다 — 답변 원가의 대부분은 입력이다.
     */
    private int chunkCount(PlanModelAssignment assignment, CostGuard guard) {
        return assignment.getChunkCount() > 0 ? assignment.getChunkCount() : guard.getDefaultChunkCount();
    }

    /**
     * 이 업체가 쓸 모델. <b>코드에 모델명이 없다</b> — 요금제별 배정에서 읽는다.
     * 배정이 없으면 답하지 않는다. 아무 모델이나 골라 쓰면 원가가 예산 밖에서 발생한다.
     */
    private PlanModelAssignment assignment(Tenant tenant) {
        return assignmentRepository.findById(tenant.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FEATURE_NOT_READY,
                        "이 요금제에 답변 모델이 배정되지 않았습니다"));
    }

    /** 근거 문서 중 경로가 있는 것(웹페이지)만 링크로 준다. 업로드 파일은 방문자가 열 수 없다. */
    private List<String> links(List<KnowledgeChunkRepository.Match> evidence) {
        return evidence.stream()
                .map(KnowledgeChunkRepository.Match::documentPath)
                .filter(path -> path != null && !path.isBlank())
                .distinct()
                .toList();
    }

    private String truncate(String text, int maxLength) {
        String stripped = text == null ? "" : text.strip();
        return stripped.length() <= maxLength ? stripped : stripped.substring(0, maxLength);
    }

    private int elapsedMs(long startedAtNanos) {
        return (int) ((System.nanoTime() - startedAtNanos) / 1_000_000L);
    }
}
