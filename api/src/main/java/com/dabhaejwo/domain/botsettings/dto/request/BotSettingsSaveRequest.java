package com.dabhaejwo.domain.botsettings.dto.request;

import com.dabhaejwo.domain.botsettings.entity.LauncherSize;

import com.dabhaejwo.domain.botsettings.entity.PageScope;
import com.dabhaejwo.domain.botsettings.entity.WidgetPosition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 챗봇 설정 저장. 전체를 한 번에 받는다(PUT) — 화면이 한 폼이고,
 * 부분 갱신으로 두면 "어느 필드를 보냈는가"를 양쪽이 계속 맞춰야 한다.
 *
 * @param brandColor        {@code #RRGGBB}. 위젯이 그대로 스타일에 넣으므로 형식을 서버에서 막는다 —
 *                          임의 문자열이 들어가면 남의 사이트에서 CSS 주입이 된다
 * @param nudgeDelaySeconds 0 이면 자동으로 말 걸지 않는다
 * @param widgetEnabled     끄면 방문자에게 위젯이 아예 뜨지 않는다(오류를 그리지 않는다)
 */
public record BotSettingsSaveRequest(
        @NotBlank @Size(max = 40) String botName,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "#RRGGBB 형식이어야 합니다")
        String brandColor,
        @NotBlank @Size(max = 200) String greeting,
        @Size(max = 2000) String persona,
        @Size(max = 300) String fallbackMessage,
        List<@Size(max = 60) String> forbiddenTopics,
        boolean leadCaptureEnabled,
        @Size(max = 40) String supportPhone,
        boolean agentHandoffEnabled,
        @Size(max = 60) String agentHours,
        @NotNull WidgetPosition widgetPosition,
        @NotNull PageScope pageScope,
        List<@Size(max = 200) String> pagePatterns,
        @Min(0) @Max(600) int nudgeDelaySeconds,
        boolean widgetEnabled,
        LauncherSize launcherSize) {
}
