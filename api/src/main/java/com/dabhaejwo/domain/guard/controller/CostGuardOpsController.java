package com.dabhaejwo.domain.guard.controller;

import com.dabhaejwo.domain.guard.dto.request.CostGuardUpdateRequest;
import com.dabhaejwo.domain.guard.dto.request.EmbeddingProviderRequest;
import com.dabhaejwo.domain.guard.dto.response.CostGuardResponse;
import com.dabhaejwo.domain.guard.service.CostGuardService;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/cost-guards")
public class CostGuardOpsController {

    private final CostGuardService service;

    public CostGuardOpsController(CostGuardService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(Permission.COST_GUARD_READ)
    public CostGuardResponse current() {
        return service.current();
    }

    @PutMapping
    @RequirePermission(Permission.COST_GUARD_WRITE)
    public CostGuardResponse update(@Valid @RequestBody CostGuardUpdateRequest request) {
        return service.update(request);
    }

    /**
     * 임베딩 공급사 교체. 안전장치 저장과 경로가 다른 이유는 결과가 전혀 다르기 때문이다 —
     * 이 호출은 <b>이미 학습된 모든 문서를 다시 학습 대상으로 만든다.</b>
     */
    @PutMapping("/embedding-provider")
    @RequirePermission(Permission.COST_GUARD_WRITE)
    public CostGuardResponse changeEmbeddingProvider(
            @Valid @RequestBody EmbeddingProviderRequest request) {
        return service.changeEmbeddingProvider(request.provider(), request.reason());
    }
}
