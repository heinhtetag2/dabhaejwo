package com.dabhaejwo.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @param path 방문자가 있던 페이지. 답변 개선 목록에서 "어느 페이지에서 물었나"로 쓰인다.
 *             전체 URL 이 아니라 경로만 받는다 — 쿼리스트링에 개인정보가 실려 올 수 있다
 */
public record AskRequest(@NotNull UUID sessionId,
                         @NotBlank @Size(max = 500) String question,
                         @Size(max = 255) String path) {
}
