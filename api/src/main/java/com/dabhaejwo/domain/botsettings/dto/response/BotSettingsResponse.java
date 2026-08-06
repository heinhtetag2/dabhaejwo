package com.dabhaejwo.domain.botsettings.dto.response;

import com.dabhaejwo.domain.botsettings.entity.LauncherBackground;
import com.dabhaejwo.domain.botsettings.entity.LauncherSize;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.botsettings.entity.PageScope;
import com.dabhaejwo.domain.botsettings.entity.WidgetPosition;

import java.util.List;

/** 챗봇 설정. api-contracts.md §9-4. */
public record BotSettingsResponse(
        String botName,
        String brandColor,
        String greeting,
        String persona,
        String fallbackMessage,
        List<String> forbiddenTopics,
        boolean leadCaptureEnabled,
        String supportPhone,
        boolean agentHandoffEnabled,
        String agentHours,
        WidgetPosition widgetPosition,
        PageScope pageScope,
        List<String> pagePatterns,
        int nudgeDelaySeconds,
        boolean widgetEnabled,
        LauncherSize launcherSize,
        LauncherBackground launcherBackground,
        String launcherIconUrl,
        String logoUrl) {

    public static BotSettingsResponse from(BotSettings settings) {
        return new BotSettingsResponse(
                settings.getBotName(),
                settings.getBrandColor(),
                settings.getGreeting(),
                settings.getPersona(),
                settings.getFallbackMessage(),
                settings.getForbiddenTopics(),
                settings.isLeadCaptureEnabled(),
                settings.getSupportPhone(),
                settings.isAgentHandoffEnabled(),
                settings.getAgentHours(),
                settings.getWidgetPosition(),
                settings.getPageScope(),
                settings.getPagePatterns(),
                settings.getNudgeDelaySeconds(),
                settings.isWidgetEnabled(),
                settings.getLauncherSize(),
                settings.getLauncherBackground(),
                settings.getLauncherIconUrl(),
                settings.getLogoUrl());
    }
}
