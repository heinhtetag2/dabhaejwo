package com.dabhaejwo.domain.chat.controller;

import com.dabhaejwo.domain.chat.dto.response.AnswerResponse;
import com.dabhaejwo.domain.chat.service.AnswerService;
import com.dabhaejwo.domain.chat.service.ChatGuard;
import com.dabhaejwo.global.security.CurrentAuth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 업체가 자기 챗봇에게 직접 물어보는 미리보기.
 *
 * <p>대화를 만들지 않는다 — 업체가 시험 삼아 던진 질문이 방문자 통계와 답변 개선 목록에
 * 섞이면 두 화면 다 못 믿게 된다.
 *
 * <p>다만 <b>원가는 진짜로 나간다.</b> 모델을 실제로 부르므로 {@code ai_usage} 에 남고
 * 일일 상한도 적용된다. 미리보기라고 공짜로 두면 그 구멍으로 원가가 샌다.
 */
@RestController
@RequestMapping("/api/app/chat")
public class ChatPreviewController {

    private final AnswerService answerService;
    private final ChatGuard guard;

    public ChatPreviewController(AnswerService answerService, ChatGuard guard) {
        this.answerService = answerService;
        this.guard = guard;
    }

    @PostMapping("/preview")
    public AnswerResponse preview(@Valid @RequestBody PreviewRequest request) {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        guard.requireWithinDailyCost(tenantId, guard.settings());

        return answerService.answer(tenantId, null, request.question(), null, guard.settings());
    }

    public record PreviewRequest(@NotBlank @Size(max = 500) String question) {
    }
}
