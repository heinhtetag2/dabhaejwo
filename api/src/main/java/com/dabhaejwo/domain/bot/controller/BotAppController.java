package com.dabhaejwo.domain.bot.controller;

import com.dabhaejwo.domain.bot.dto.request.BotSaveRequest;
import com.dabhaejwo.domain.bot.dto.response.BotResponse;
import com.dabhaejwo.domain.bot.service.BotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 서비스 관리. 화면에서는 "서비스", 코드·API 에서는 {@code bot} 이다.
 *
 * <p>삭제는 아직 없다 — 되돌릴 수 없는 행위라 나머지가 안정된 뒤에 붙인다
 * ({@code docs/plan/service-plan.md} §11 M4).
 */
@RestController
@RequestMapping("/api/app/bots")
public class BotAppController {

    private final BotService botService;

    public BotAppController(BotService botService) {
        this.botService = botService;
    }

    @GetMapping
    public List<BotResponse> list() {
        return botService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BotResponse create(@Valid @RequestBody BotSaveRequest request) {
        return botService.create(request);
    }

    /**
     * 단건 조회.
     *
     * <p>경로에 {@code botId} 가 있으므로 {@code BotScopeInterceptor} 가 소유 업체를 먼저
     * 대조한다 — 남의 서비스면 여기 도달하지 않고 404 다.
     */
    @GetMapping("/{botId}")
    public BotResponse get(@PathVariable UUID botId) {
        return botService.get(botId);
    }

    @PatchMapping("/{botId}")
    public BotResponse update(@PathVariable UUID botId, @Valid @RequestBody BotSaveRequest request) {
        return botService.update(botId, request);
    }
}
