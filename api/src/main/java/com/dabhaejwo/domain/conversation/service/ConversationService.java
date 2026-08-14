package com.dabhaejwo.domain.conversation.service;

import com.dabhaejwo.domain.conversation.dto.response.ConversationDetailResponse;
import com.dabhaejwo.domain.conversation.dto.response.ConversationSummaryResponse;
import com.dabhaejwo.domain.conversation.entity.Conversation;
import com.dabhaejwo.domain.conversation.entity.Message;
import com.dabhaejwo.domain.conversation.repository.ConversationRepository;
import com.dabhaejwo.domain.conversation.repository.MessageRepository;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.common.PageResponse;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.domain.bot.service.BotContext;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 대화 로그.
 *
 * <p><b>대리 접속 세션이 대화를 열면 감사 기록을 남긴다.</b> 남의 고객이 챗봇과 나눈 대화를
 * 운영자가 읽는 행위이므로, 편의보다 통제를 우선한다 (api-contracts.md §9-3).
 */
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final BotContext botContext;
    private final MessageRepository messageRepository;
    private final AuditLogService auditLogService;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               AuditLogService auditLogService,
                       BotContext botContext) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auditLogService = auditLogService;
        this.botContext = botContext;
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationSummaryResponse> list(String q, int page, Integer size) {
        UUID botId = botContext.scope().botId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size));

        Page<Conversation> conversations = (q == null || q.isBlank())
                ? conversationRepository.findAnsweredByBotId(botId, pageable)
                : conversationRepository.search(botId, q.strip(), pageable);

        List<UUID> ids = conversations.getContent().stream().map(Conversation::getId).toList();
        Map<UUID, String> previews = previews(botId, ids);
        Set<UUID> failed = failedConversationIds(botId, ids);

        Map<UUID, Integer> numbers = visitorNumbers(botId);

        return PageResponse.of(conversations, conversation ->
                ConversationSummaryResponse.of(
                        conversation,
                        previews.get(conversation.getId()),
                        failed.contains(conversation.getId()),
                        numbers.getOrDefault(conversation.getId(), 0)));
    }

    /**
     * 방문자 번호. <b>같은 IP 해시는 같은 번호</b>다.
     *
     * <p>순번을 그냥 매기면 한 사람이 세 번 열었을 때 방문자 1·2·3 으로 보여 실제보다 많아
     * 보인다. 우리는 IP 원문을 저장하지 않으므로(해시만 남는다) 이 번호로도 그가 누구인지는
     * 알 수 없다 — 업체 안에서 "같은 사람인가"만 답한다.
     *
     * <p>번호는 <b>처음 온 순서</b>다. 오래된 방문자가 1번이라 나중에 대화가 늘어도 번호가
     * 바뀌지 않는다 — 최근 순으로 매기면 새 방문자가 올 때마다 전부 밀린다.
     *
     * <p>업체 전체를 훑는다. 페이지 단위로 매기면 2페이지의 같은 방문자가 다른 번호를 받는다.
     */
    private Map<UUID, Integer> visitorNumbers(UUID botId) {
        Map<String, Integer> byVisitor = new LinkedHashMap<>();
        Map<UUID, Integer> byConversation = new LinkedHashMap<>();

        for (Conversation conversation : conversationRepository
                .findAllByBotIdOrderByStartedAtAsc(botId)) {
            // 해시가 없는 옛 대화는 각각 다른 방문자로 본다 — 묶을 근거가 없다.
            String visitor = conversation.getVisitorIpHash() == null
                    ? "conversation:" + conversation.getId()
                    : conversation.getVisitorIpHash();
            int number = byVisitor.computeIfAbsent(visitor, key -> byVisitor.size() + 1);
            byConversation.put(conversation.getId(), number);
        }
        return byConversation;
    }

    @Transactional
    public ConversationDetailResponse detail(UUID conversationId) {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();
        Conversation conversation = conversationRepository
                .findByIdAndBotId(conversationId, botContext.scope().botId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (user.impersonating()) {
            // 운영자가 남의 고객 대화를 열었다. 이 기록은 수정·삭제할 수 없다.
            auditLogService.recordImpersonatedRead(
                    user.impersonatingOperatorId(), user.tenantId(),
                    user.impersonationSessionId(), conversationId);
        }

        return ConversationDetailResponse.of(conversation,
                messageRepository.findAllByBotIdAndConversationIdOrderByCreatedAtAsc(
                        botContext.scope().botId(), conversationId),
                // 목록과 같은 번호여야 한다 — 같은 규칙으로 다시 매긴다.
                visitorNumbers(botContext.scope().botId()).getOrDefault(conversationId, 0));
    }

    /** 목록의 미리보기 — 각 대화의 첫 방문자 발화. 한 번의 질의로 모아 N+1 을 피한다. */
    private Map<UUID, String> previews(UUID botId, List<UUID> conversationIds) {
        Map<UUID, String> previews = new HashMap<>();
        if (conversationIds.isEmpty()) {
            return previews;
        }
        for (Message message : messageRepository.findVisitorMessages(botId, conversationIds)) {
            // 시간 오름차순이므로 먼저 들어온 것이 첫 발화다.
            previews.putIfAbsent(message.getConversationId(), message.getContent());
        }
        return previews;
    }

    /** 답변 실패가 하나라도 있는 대화. 목록에서 배지로 표시해 개선으로 유도한다. */
    private Set<UUID> failedConversationIds(UUID botId, List<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(messageRepository.findFailedConversationIds(botId, conversationIds));
    }
}
