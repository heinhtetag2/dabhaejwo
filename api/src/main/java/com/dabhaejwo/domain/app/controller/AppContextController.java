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

    /**
     * 홈은 <b>서비스별</b> 화면이라 경로에 서비스가 실린다.
     *
     * <p>{@code /me} 는 업체 단위로 남는다 — 로그인한 사람과 계약은 서비스를 가리지 않는다.
     */
    @GetMapping("/bots/{botId}/home")
    public HomeSummaryResponse home() {
        return homeService.summary();
    }
}
