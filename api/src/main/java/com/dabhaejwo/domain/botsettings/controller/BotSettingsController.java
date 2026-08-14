package com.dabhaejwo.domain.botsettings.controller;

import com.dabhaejwo.domain.botsettings.dto.request.BotSettingsSaveRequest;
import com.dabhaejwo.domain.botsettings.dto.response.BotSettingsResponse;
import com.dabhaejwo.domain.botsettings.service.BotSettingsService;
import com.dabhaejwo.domain.tenant.dto.request.AllowedOriginRequest;
import com.dabhaejwo.domain.tenant.dto.response.AllowedOriginResponse;
import com.dabhaejwo.domain.tenant.service.AllowedOriginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 말투와 모양 · 설치 화면이 쓰는 엔드포인트. */
@RestController
@RequestMapping("/api/app/bots/{botId}")
public class BotSettingsController {

    private final BotSettingsService botSettingsService;
    private final AllowedOriginService allowedOriginService;

    public BotSettingsController(BotSettingsService botSettingsService,
                                 AllowedOriginService allowedOriginService) {
        this.botSettingsService = botSettingsService;
        this.allowedOriginService = allowedOriginService;
    }

    @GetMapping("/appearance")
    public BotSettingsResponse appearance() {
        return botSettingsService.current();
    }

    @PutMapping("/appearance")
    public BotSettingsResponse saveAppearance(@Valid @RequestBody BotSettingsSaveRequest request) {
        return botSettingsService.save(request);
    }

    @GetMapping("/allowed-origins")
    public List<AllowedOriginResponse> origins() {
        return allowedOriginService.list();
    }

    @PostMapping("/allowed-origins")
    @ResponseStatus(HttpStatus.CREATED)
    public AllowedOriginResponse addOrigin(@Valid @RequestBody AllowedOriginRequest request) {
        return allowedOriginService.add(request.origin());
    }

    @DeleteMapping("/allowed-origins/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeOrigin(@PathVariable UUID id) {
        allowedOriginService.remove(id);
    }
}
