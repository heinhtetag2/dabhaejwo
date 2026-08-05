package com.dabhaejwo.domain.usage.dto.response;

import com.dabhaejwo.global.common.PageResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 수익성 화면. 지표와 목록을 함께 준다 — 두 번 부를 이유가 없다.
 *
 * <p>{@code costRatioWarnPercent} 를 싣는 이유는 경고선이 {@code cost_guards} 설정값이기
 * 때문이다. 프론트가 70을 상수로 갖고 있으면 설정을 바꿔도 색이 안 바뀐다.
 */
public record ProfitabilityResponse(
        Stats stats,
        List<Item> content,
        PageResponse.PageInfo page) {

    public record Stats(
            long revenueKrw,
            BigDecimal costKrw,
            int avgCostRatioPercent,
            int savedAnswerPercent,
            long costExceededCount,
            int costRatioWarnPercent) {
    }

    public record Item(
            TenantRef tenant,
            String planName,
            int billedKrw,
            BigDecimal costKrw,
            int costRatioPercent,
            int savedAnswerPercent) {
    }

    public record TenantRef(UUID id, String name) {
    }
}
