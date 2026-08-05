package com.dabhaejwo.domain.provider.controller;

import com.dabhaejwo.domain.provider.dto.request.ProviderCredentialRequest;
import com.dabhaejwo.domain.provider.dto.request.ProviderEnabledRequest;
import com.dabhaejwo.domain.provider.dto.response.ProviderCredentialResponse;
import com.dabhaejwo.domain.provider.service.ProviderCredentialService;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.security.Permission;
import com.dabhaejwo.global.security.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공급사 연결. <b>키를 되읽는 엔드포인트가 없다</b> — 한 번 넣으면 사람이 다시 볼 수 없고
 * 교체만 할 수 있다. 조회할 수 있으면 콘솔 계정 하나가 뚫렸을 때 키까지 함께 나간다.
 */
@RestController
@RequestMapping("/api/ops/provider-credentials")
public class ProviderCredentialOpsController {

    private final ProviderCredentialService service;

    public ProviderCredentialOpsController(ProviderCredentialService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(Permission.PROVIDER_CREDENTIAL_READ)
    public List<ProviderCredentialResponse> list() {
        return service.list();
    }

    @PutMapping("/{provider}")
    @RequirePermission(Permission.PROVIDER_CREDENTIAL_WRITE)
    public ProviderCredentialResponse save(@PathVariable LlmProviderName provider,
                                           @Valid @RequestBody ProviderCredentialRequest request) {
        return service.save(provider, request);
    }

    @PatchMapping("/{provider}")
    @RequirePermission(Permission.PROVIDER_CREDENTIAL_WRITE)
    public ProviderCredentialResponse changeEnabled(@PathVariable LlmProviderName provider,
                                                    @Valid @RequestBody ProviderEnabledRequest request) {
        return service.changeEnabled(provider, request.enabled(), request.reason());
    }
}
