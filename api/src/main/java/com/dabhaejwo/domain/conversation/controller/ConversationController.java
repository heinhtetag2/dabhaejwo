package com.dabhaejwo.domain.conversation.controller;

import com.dabhaejwo.domain.conversation.dto.response.ConversationDetailResponse;
import com.dabhaejwo.domain.conversation.dto.response.ConversationSummaryResponse;
import com.dabhaejwo.domain.conversation.service.ConversationService;
import com.dabhaejwo.global.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/app/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public PageResponse<ConversationSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        return conversationService.list(q, page, size);
    }

    /** 대리 접속 세션이 호출하면 감사 기록이 남는다. */
    @GetMapping("/{id}")
    public ConversationDetailResponse detail(@PathVariable UUID id) {
        return conversationService.detail(id);
    }
}
