package com.dabhaejwo.domain.provider.service;

import com.dabhaejwo.domain.operator.service.OperatorLookupService;
import com.dabhaejwo.domain.provider.dto.request.ProviderCredentialRequest;
import com.dabhaejwo.domain.provider.dto.response.ProviderCredentialResponse;
import com.dabhaejwo.domain.provider.entity.ProviderCredential;
import com.dabhaejwo.domain.provider.repository.ProviderCredentialRepository;
import com.dabhaejwo.global.audit.AuditAction;
import com.dabhaejwo.global.audit.AuditLogService;
import com.dabhaejwo.global.config.AppProperties;
import com.dabhaejwo.global.crypto.SecretCipher;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 공급사 자격증명 관리.
 *
 * <p>키를 콘솔에서 바꿀 수 있게 만든 이유는 <b>노출된 키를 재배포 없이 교체</b>하기
 * 위해서다. 환경변수만 쓰던 시절에는 급히 갈아끼울 방법이 없었다.
 *
 * <p>우선순위는 <b>콘솔 등록분 → 환경변수</b>다. 환경변수를 남겨 둔 이유는 이미 그렇게
 * 돌던 환경을 깨지 않기 위해서다. 다만 어느 쪽이 쓰이고 있는지 화면에 드러낸다 —
 * "분명 새 키를 넣었는데 옛 키로 돈다"를 눈으로 알 수 있어야 한다.
 */
@Service
public class ProviderCredentialService {

    /** 자격증명이 필요한 공급사. STUB 은 키가 없으므로 여기 없다. */
    private static final List<LlmProviderName> MANAGED = List.of(
            LlmProviderName.GOOGLE, LlmProviderName.ANTHROPIC, LlmProviderName.OPENAI);

    private final ProviderCredentialRepository repository;
    private final SecretCipher cipher;
    private final OperatorLookupService operatorLookup;
    private final AuditLogService auditLogService;
    private final AppProperties.Llm llmProperties;

    public ProviderCredentialService(ProviderCredentialRepository repository,
                                     SecretCipher cipher,
                                     OperatorLookupService operatorLookup,
                                     AuditLogService auditLogService,
                                     AppProperties properties) {
        this.repository = repository;
        this.cipher = cipher;
        this.operatorLookup = operatorLookup;
        this.auditLogService = auditLogService;
        this.llmProperties = properties.llm();
    }

    /** 관리 대상 공급사 전부. 등록되지 않은 공급사도 "미설정"으로 한 줄 나온다. */
    @Transactional(readOnly = true)
    public List<ProviderCredentialResponse> list() {
        Map<LlmProviderName, ProviderCredential> stored = new java.util.EnumMap<>(LlmProviderName.class);
        repository.findAllByOrderByProviderAsc().forEach(row -> stored.put(row.getProvider(), row));

        Map<UUID, String> names = operatorLookup.namesOf(
                stored.values().stream().map(ProviderCredential::getUpdatedBy).toList());

        List<ProviderCredentialResponse> result = new ArrayList<>();
        for (LlmProviderName provider : MANAGED) {
            ProviderCredential credential = stored.get(provider);
            if (credential != null) {
                result.add(new ProviderCredentialResponse(
                        provider,
                        true,
                        credential.isEnabled(),
                        credential.getKeyHint(),
                        ProviderCredentialResponse.Source.CONSOLE,
                        credential.getUpdatedAt(),
                        operatorLookup.nameOf(names, credential.getUpdatedBy())));
                continue;
            }
            boolean fromEnv = envKey(provider) != null;
            result.add(new ProviderCredentialResponse(
                    provider,
                    fromEnv,
                    fromEnv,
                    fromEnv ? "환경변수" : null,
                    fromEnv ? ProviderCredentialResponse.Source.ENV
                            : ProviderCredentialResponse.Source.NONE,
                    null,
                    null));
        }
        return result;
    }

    @Transactional
    public ProviderCredentialResponse save(LlmProviderName provider, ProviderCredentialRequest request) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        requireManaged(provider);

        if (!cipher.available()) {
            // 평문으로 저장하느니 거부한다. 안전한 것처럼 보이는 상태로 운영에 나가면 안 된다.
            throw new BusinessException(ErrorCode.ENCRYPTION_UNAVAILABLE,
                    "ENCRYPTION_KEY 를 설정해야 공급사 키를 저장할 수 있습니다");
        }

        String apiKey = request.apiKey().strip();
        String encrypted = cipher.encrypt(apiKey);
        String hint = cipher.hint(apiKey);

        ProviderCredential credential = repository.findById(provider)
                .map(existing -> {
                    existing.replaceKey(encrypted, hint, operator.operatorId());
                    return existing;
                })
                .orElseGet(() -> repository.save(ProviderCredential.register(
                        provider, encrypted, hint, operator.operatorId())));

        // 사유·행위자는 남기되 키는 어디에도 남기지 않는다. meta 에 힌트조차 넣지 않는다.
        auditLogService.record(operator.operatorId(), AuditAction.PROVIDER_CREDENTIAL_WRITE, null,
                request.reason(), Map.of("provider", provider.name(), "action", "SAVE_KEY"));

        return list().stream()
                .filter(row -> row.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("방금 저장한 자격증명을 찾지 못했습니다"));
    }

    @Transactional
    public ProviderCredentialResponse changeEnabled(LlmProviderName provider, boolean enabled, String reason) {
        AuthPrincipal.Operator operator = CurrentAuth.operator();
        requireManaged(provider);

        ProviderCredential credential = repository.findById(provider)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDENTIAL_NOT_FOUND));
        credential.changeEnabled(enabled, operator.operatorId());

        auditLogService.record(operator.operatorId(), AuditAction.PROVIDER_CREDENTIAL_WRITE, null,
                reason, Map.of("provider", provider.name(), "enabled", enabled));

        return list().stream()
                .filter(row -> row.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("방금 바꾼 자격증명을 찾지 못했습니다"));
    }

    /**
     * 실제로 쓸 키. <b>공급사 어댑터만 호출한다.</b>
     *
     * <p>콘솔 등록분이 우선이고, 없으면 환경변수로 떨어진다. 둘 다 없으면 null 이며
     * 어댑터가 {@code available() = false} 로 답해 게이트웨이가 그 공급사를 고르지 않는다.
     */
    @Transactional(readOnly = true)
    public String resolveKey(LlmProviderName provider) {
        ProviderCredential credential = repository.findById(provider).orElse(null);
        if (credential != null && credential.isEnabled()) {
            return cipher.decrypt(credential.getApiKeyCipher());
        }
        // 콘솔에 등록됐지만 꺼 둔 경우에는 환경변수로 되돌아가지 않는다 —
        // 끈 것은 "이 공급사를 쓰지 말라"는 뜻이지 "옛 키로 돌아가라"가 아니다.
        if (credential != null) {
            return null;
        }
        return envKey(provider);
    }

    /** 복호화까지는 하지 않고 "쓸 수 있는가"만 본다. 어댑터의 {@code available()} 이 자주 부른다. */
    @Transactional(readOnly = true)
    public boolean configured(LlmProviderName provider) {
        ProviderCredential credential = repository.findById(provider).orElse(null);
        if (credential != null) {
            return credential.isEnabled() && cipher.available();
        }
        return envKey(provider) != null;
    }

    private String envKey(LlmProviderName provider) {
        AppProperties.Llm.Credential credential = switch (provider) {
            case GOOGLE -> llmProperties.google();
            case ANTHROPIC -> llmProperties.anthropic();
            case OPENAI -> llmProperties.openai();
            case STUB -> null;
        };
        return credential != null && credential.configured() ? credential.apiKey() : null;
    }

    private void requireManaged(LlmProviderName provider) {
        if (!MANAGED.contains(provider)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    provider + " 는 자격증명을 두지 않는 공급사입니다");
        }
    }
}
