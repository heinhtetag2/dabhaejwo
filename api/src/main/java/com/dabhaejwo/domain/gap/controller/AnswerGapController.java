package com.dabhaejwo.domain.gap.controller;

import com.dabhaejwo.domain.gap.dto.request.GapResolveRequest;
import com.dabhaejwo.domain.gap.dto.response.AnswerGapResponse;
import com.dabhaejwo.domain.gap.entity.GapStatus;
import com.dabhaejwo.domain.gap.service.AnswerGapService;
import com.dabhaejwo.global.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/bots/{botId}/answer-gaps")
public class AnswerGapController {

    private final AnswerGapService answerGapService;

    public AnswerGapController(AnswerGapService answerGapService) {
        this.answerGapService = answerGapService;
    }

    @GetMapping
    public PageResponse<AnswerGapResponse> list(@RequestParam(required = false) GapStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(required = false) Integer size) {
        return answerGapService.list(status, page, size);
    }

    @PostMapping("/{id}/resolve")
    public AnswerGapResponse resolve(@PathVariable Long id,
                                     @Valid @RequestBody GapResolveRequest request) {
        return answerGapService.resolve(id, request);
    }

    @PostMapping("/{id}/dismiss")
    public AnswerGapResponse dismiss(@PathVariable Long id) {
        return answerGapService.dismiss(id);
    }
}
