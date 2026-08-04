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
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
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
    private final MessageRepository messageRepository;
    private final AuditLogService auditLogService;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               AuditLogService auditLogService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationSummaryResponse> list(String q, int page, Integer size) {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size));

        Page<Conversation> conversations = (q == null || q.isBlank())
                ? conversationRepository.findAllByTenantIdOrderByStartedAtDesc(tenantId, pageable)
                : conversationRepository.search(tenantId, q.strip(), pageable);

        List<UUID> ids = conversations.getContent().stream().map(Conversation::getId).toList();
        Map<UUID, String> previews = previews(tenantId, ids);
        Set<UUID> failed = failedConversationIds(tenantId, ids);

        return PageResponse.of(conversations, conversation ->
                ConversationSummaryResponse.of(
                        conversation,
                        previews.get(conversation.getId()),
                        failed.contains(conversation.getId())));
    }

    @Transactional
    public ConversationDetailResponse detail(UUID conversationId) {
        AuthPrincipal.TenantUser user = CurrentAuth.tenantUser();
        Conversation conversation = conversationRepository
                .findByIdAndTenantId(conversationId, user.tenantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (user.impersonating()) {
            // 운영자가 남의 고객 대화를 열었다. 이 기록은 수정·삭제할 수 없다.
            auditLogService.recordImpersonatedRead(
                    user.impersonatingOperatorId(), user.tenantId(),
                    user.impersonationSessionId(), conversationId);
        }

        return ConversationDetailResponse.of(conversation,
                messageRepository.findAllByTenantIdAndConversationIdOrderByCreatedAtAsc(
                        user.tenantId(), conversationId));
    }

    /** 목록의 미리보기 — 각 대화의 첫 방문자 발화. 한 번의 질의로 모아 N+1 을 피한다. */
    private Map<UUID, String> previews(UUID tenantId, List<UUID> conversationIds) {
        Map<UUID, String> previews = new HashMap<>();
        if (conversationIds.isEmpty()) {
            return previews;
        }
        for (Message message : messageRepository.findVisitorMessages(tenantId, conversationIds)) {
            // 시간 오름차순이므로 먼저 들어온 것이 첫 발화다.
            previews.putIfAbsent(message.getConversationId(), message.getContent());
        }
        return previews;
    }

    /** 답변 실패가 하나라도 있는 대화. 목록에서 배지로 표시해 개선으로 유도한다. */
    private Set<UUID> failedConversationIds(UUID tenantId, List<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(messageRepository.findFailedConversationIds(tenantId, conversationIds));
    }
}
