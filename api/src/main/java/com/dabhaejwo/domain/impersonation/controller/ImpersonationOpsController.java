package com.dabhaejwo.domain.impersonation.controller;

import com.dabhaejwo.domain.impersonation.dto.request.ImpersonationRequest;
import com.dabhaejwo.domain.impersonation.dto.response.ImpersonationSessionResponse;
import com.dabhaejwo.domain.impersonation.service.ImpersonationOpsService;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ops")
public class ImpersonationOpsController {

    private final ImpersonationOpsService service;

    public ImpersonationOpsController(ImpersonationOpsService service) {
        this.service = service;
    }

    @PostMapping("/tenants/{tenantId}/impersonate")
    @RequirePermission(Permission.TENANT_IMPERSONATE)
    public ImpersonationSessionResponse start(@PathVariable UUID tenantId,
                                              @Valid @RequestBody ImpersonationRequest request) {
        return service.start(tenantId, request);
    }

    @PostMapping("/impersonations/{sessionId}/extend")
    @RequirePermission(Permission.TENANT_IMPERSONATE)
    public ImpersonationSessionResponse extend(@PathVariable UUID sessionId,
                                               @Valid @RequestBody ImpersonationRequest request) {
        return service.extend(sessionId, request);
    }

    /** 종료는 권한을 걸지 않는다 — 자기가 연 세션을 닫는 것을 막을 이유가 없다. */
    @DeleteMapping("/impersonations/{sessionId}")
    public ImpersonationSessionResponse end(@PathVariable UUID sessionId) {
        return service.end(sessionId);
    }
}
