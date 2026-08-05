package com.dabhaejwo.domain.today.controller;

import com.dabhaejwo.domain.today.dto.response.TodaySummaryResponse;
import com.dabhaejwo.domain.today.service.TodayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 진입 화면. <b>권한을 걸지 않는다</b> — 전 역할이 본다 (admin-console-plan.md §7).
 * 인증은 필터 체인이 이미 요구한다.
 */
@RestController
@RequestMapping("/api/ops/today")
public class TodayOpsController {

    private final TodayService service;

    public TodayOpsController(TodayService service) {
        this.service = service;
    }

    @GetMapping
    public TodaySummaryResponse summary() {
        return service.summary();
    }
}
