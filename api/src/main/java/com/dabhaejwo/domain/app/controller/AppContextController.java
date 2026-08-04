package com.dabhaejwo.domain.app.controller;

import com.dabhaejwo.domain.app.dto.response.AppContextResponse;
import com.dabhaejwo.domain.app.service.AppContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppContextController {

    private final AppContextService appContextService;

    public AppContextController(AppContextService appContextService) {
        this.appContextService = appContextService;
    }

    @GetMapping("/me")
    public AppContextResponse me() {
        appContextService.touchLastSeen();
        return appContextService.current();
    }
}
