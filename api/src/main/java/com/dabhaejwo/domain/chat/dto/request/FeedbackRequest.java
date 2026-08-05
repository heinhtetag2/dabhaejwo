package com.dabhaejwo.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @param helpful 도움이 됐는가. {@code false} 면 그 질문이 답변 개선 목록으로 올라간다 —
 *                답은 했지만 틀렸다는 신호이고, 업체가 알아야 할 것은 이쪽이 더 많다
 */
public record FeedbackRequest(@NotNull UUID messageId, @NotNull Boolean helpful) {
}
