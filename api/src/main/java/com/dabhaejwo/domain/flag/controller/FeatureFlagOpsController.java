package com.dabhaejwo.domain.flag.controller;

import com.dabhaejwo.domain.flag.dto.request.FeatureFlagUpdateRequest;
import com.dabhaejwo.domain.flag.dto.response.FeatureFlagResponse;
import com.dabhaejwo.domain.flag.service.FeatureFlagService;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ops/feature-flags")
public class FeatureFlagOpsController {

    private final FeatureFlagService service;

    public FeatureFlagOpsController(FeatureFlagService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(Permission.FLAG_READ)
    public List<FeatureFlagResponse> list() {
        return service.list();
    }

    @PatchMapping("/{key}")
    @RequirePermission(Permission.FLAG_WRITE)
    public FeatureFlagResponse update(@PathVariable String key,
                                      @Valid @RequestBody FeatureFlagUpdateRequest request) {
        return service.update(key, request);
    }
}
