package com.dabhaejwo.domain.conversation.dto.response;

import com.dabhaejwo.domain.conversation.entity.Conversation;
import com.dabhaejwo.domain.conversation.entity.Message;
import com.dabhaejwo.domain.conversation.entity.MessageRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 대화 상세. Summary 의 상위집합이며 겹치는 필드는 이름·타입이 완전히 같다.
 *
 * <p>답을 못 한 말풍선을 화면이 따로 표시하고 "여기에 답 달기"를 붙일 수 있도록
 * {@code answered} 를 그대로 내려준다.
 */
public record ConversationDetailResponse(
        UUID id,
        String visitorRegion,
        String startedPath,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        List<MessageItem> messages) {

    /**
     * @param answered BOT 메시지만 값이 있다. false 면 답변 실패
     * @param saved    저장 답변(공통 질문)으로 나갔는가. true 면 모델을 거치지 않았다
     */
    public record MessageItem(
            UUID id,
            MessageRole role,
            String content,
            Boolean answered,
            boolean saved,
            OffsetDateTime createdAt) {

        static MessageItem from(Message message) {
            return new MessageItem(
                    message.getId(),
                    message.getRole(),
                    message.getContent(),
                    message.getAnswered(),
                    message.isSaved(),
                    message.getCreatedAt());
        }
    }

    public static ConversationDetailResponse of(Conversation conversation, List<Message> messages) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getVisitorRegion(),
                conversation.getStartedPath(),
                conversation.getStartedAt(),
                conversation.getEndedAt(),
                messages.stream().map(MessageItem::from).toList());
    }
}
