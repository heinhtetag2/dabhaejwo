package com.dabhaejwo.domain.conversation.dto.response;

import com.dabhaejwo.domain.conversation.entity.Conversation;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 대화 목록의 한 줄.
 *
 * @param preview 첫 방문자 발화. 없으면 null — 열기만 하고 아무 말 없이 닫은 대화가 있다
 */
public record ConversationSummaryResponse(
        UUID id,
        String visitorRegion,
        String startedPath,
        OffsetDateTime startedAt,
        String preview,
        boolean hasFailure) {

    public static ConversationSummaryResponse of(Conversation conversation, String preview, boolean hasFailure) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getVisitorRegion(),
                conversation.getStartedPath(),
                conversation.getStartedAt(),
                preview,
                hasFailure);
    }
}
