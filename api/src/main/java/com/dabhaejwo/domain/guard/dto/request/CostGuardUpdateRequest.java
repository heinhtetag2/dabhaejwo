package com.dabhaejwo.domain.guard.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * 안전장치 갱신. PUT 이라 전체 필드를 받는다 — 부분 수정을 허용하면 화면에서 안 보이던
 * 값이 조용히 유지되거나 덮이는데, 여기 있는 값은 전부 서비스 가용성이나 원가에 직결된다.
 *
 * <p>하한을 두는 이유는 0 이 "제한 없음"이 아니라 "전부 차단"으로 동작하기 때문이다.
 *
 * @param reason 사유 필수. 상한을 잘못 만지면 챗봇이 전면 중단된다
 */
public record CostGuardUpdateRequest(
        @Min(1) int tenantDailyCapKrw,
        @Min(1) int globalDailyCapKrw,
        @Min(1) int ipQuestionsPerMin,
        @Min(1) int bulkUploadLimit,
        @Min(1) @Max(500) int costRatioWarnPercent,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal answerFailSimilarity,
        // 조각 수는 입력 토큰에 비례한다. 상한이 없으면 실수 한 번으로 원가가 몇 배가 된다.
        @Min(1) @Max(20) int defaultChunkCount,
        @Min(50) @Max(4000) int answerMaxLength,
        @Min(0) @Max(365) int churnPurgeGraceDays,
        @Pattern(regexp = "STOP_AND_NOTICE|OVERAGE_BILLING|NOTIFY_ONLY")
        String quotaExceededBehavior,
        boolean slackAlertEnabled,
        @NotNull String commonPrompt,
        @NotBlank String reason) {
}
