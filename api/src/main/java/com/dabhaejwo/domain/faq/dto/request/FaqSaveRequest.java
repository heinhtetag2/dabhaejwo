package com.dabhaejwo.domain.faq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 공통 질문 등록·수정. 생성과 수정이 같은 모양이라 하나로 둔다.
 *
 * @param question 버튼 한 줄에 들어가야 한다. 20자 권장이지만 강제하지 않는다 —
 *                 권장을 넘겼다고 저장을 막으면 업체가 우회 문구를 만들어 낸다
 */
public record FaqSaveRequest(
        @NotBlank @Size(max = 120) String question,
        @NotBlank @Size(max = 4000) String answer,
        List<String> links,
        List<UUID> followUpFaqIds,
        boolean shown) {
}
