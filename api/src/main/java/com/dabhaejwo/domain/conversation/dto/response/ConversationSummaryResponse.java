package com.dabhaejwo.domain.conversation.dto.response;

import com.dabhaejwo.domain.conversation.entity.Conversation;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 대화 목록의 한 줄.
 *
 * @param preview       첫 방문자 발화. 없으면 null — 열기만 하고 아무 말 없이 닫은 대화가 있다
 * @param visitorNumber 업체 안에서만 뜻이 있는 방문자 번호. <b>같은 방문자는 같은 번호</b>다 —
 *                      한 사람이 세 번 열었을 때 1·2·3 으로 보이면 방문자가 셋인 줄 안다.
 *                      IP 해시로 묶으므로 우리는 그가 누구인지 알지 못한다
 */
public record ConversationSummaryResponse(
        UUID id,
        String visitorRegion,
        String startedPath,
        OffsetDateTime startedAt,
        String preview,
        boolean hasFailure,
        int visitorNumber) {

    public static ConversationSummaryResponse of(Conversation conversation, String preview,
                                                 boolean hasFailure, int visitorNumber) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getVisitorRegion(),
                conversation.getStartedPath(),
                conversation.getStartedAt(),
                preview,
                hasFailure,
                visitorNumber);
    }
}
