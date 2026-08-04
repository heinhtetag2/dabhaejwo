package com.dabhaejwo.domain.app.controller;

import com.dabhaejwo.domain.app.dto.response.AppContextResponse;
import com.dabhaejwo.domain.app.dto.response.HomeSummaryResponse;
import com.dabhaejwo.domain.app.service.AppContextService;
import com.dabhaejwo.domain.app.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
public class AppContextController {

    private final AppContextService appContextService;
    private final HomeService homeService;

    public AppContextController(AppContextService appContextService, HomeService homeService) {
        this.appContextService = appContextService;
        this.homeService = homeService;
    }

    @GetMapping("/me")
    public AppContextResponse me() {
        appContextService.touchLastSeen();
        return appContextService.current();
    }

    @GetMapping("/home")
    public HomeSummaryResponse home() {
        return homeService.summary();
    }
}
