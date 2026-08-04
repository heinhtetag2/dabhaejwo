package com.dabhaejwo.domain.gap.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 답변 등록. 이 답이 공통 질문으로 승격되어 다음 질문부터 바로 쓰인다.
 *
 * @param question 대표 질문을 다듬어 저장할 수 있다. 비우면 원문을 그대로 쓴다 —
 *                 방문자가 실제로 친 말이 버튼 문구로 어울리지 않는 경우가 많다
 */
public record GapResolveRequest(
        @NotBlank @Size(max = 4000) String answer,
        @Size(max = 120) String question) {
}
