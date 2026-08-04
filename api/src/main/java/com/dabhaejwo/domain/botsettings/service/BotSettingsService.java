package com.dabhaejwo.domain.botsettings.service;

import com.dabhaejwo.domain.botsettings.dto.request.BotSettingsSaveRequest;
import com.dabhaejwo.domain.botsettings.dto.response.BotSettingsResponse;
import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.repository.BotSettingsRepository;
import com.dabhaejwo.domain.tenant.entity.Tenant;
import com.dabhaejwo.domain.tenant.repository.TenantRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.security.AuthPrincipal;
import com.dabhaejwo.global.security.CurrentAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 챗봇의 말투·모양·설치 설정.
 *
 * <p>설정이 없는 업체는 없다 — 조회 시점에 기본값으로 만들어 준다. 화면이 "설정 없음"
 * 상태를 다루지 않아도 되게 하려는 것이다.
 */
@Service
public class BotSettingsService {

    private final BotSettingsRepository settingsRepository;
    private final TenantRepository tenantRepository;

    public BotSettingsService(BotSettingsRepository settingsRepository,
                              TenantRepository tenantRepository) {
        this.settingsRepository = settingsRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public BotSettingsResponse current() {
        UUID tenantId = CurrentAuth.tenantUser().tenantId();
        return BotSettingsResponse.from(findOrCreate(tenantId));
    }

    @Transactional
    public BotSettingsResponse save(BotSettingsSaveRequest request) {
        AuthPrincipal.TenantUser user = CurrentAuth.requireEditor();
        BotSettings settings = findOrCreate(user.tenantId());

        settings.editAppearance(request.botName().strip(), request.brandColor(),
                request.greeting().strip());
        settings.editTone(nullToEmpty(request.persona()), nullToEmpty(request.fallbackMessage()),
                clean(request.forbiddenTopics()));
        settings.editFallback(request.leadCaptureEnabled(), blankToNull(request.supportPhone()),
                request.agentHandoffEnabled(), blankToNull(request.agentHours()));
        settings.editPlacement(request.widgetPosition(), request.pageScope(),
                clean(request.pagePatterns()), request.nudgeDelaySeconds());

        return BotSettingsResponse.from(settings);
    }

    private BotSettings findOrCreate(UUID tenantId) {
        return settingsRepository.findById(tenantId).orElseGet(() -> {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
            return settingsRepository.save(BotSettings.defaults(tenantId, tenant.getName()));
        });
    }

    /** 빈 항목과 앞뒤 공백을 걷어낸다. 쉼표 입력에서 "a, , b" 같은 값이 흔히 들어온다. */
    private List<String> clean(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.strip();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
