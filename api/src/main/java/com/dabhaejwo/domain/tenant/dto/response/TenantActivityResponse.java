package com.dabhaejwo.domain.tenant.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 업체 활동 이력 한 줄. api-contracts.md §2-5.
 *
 * <p>감사 기록·결제 기록·내부 메모를 시각 역순으로 합친 <b>읽기 전용 합성 뷰</b>다.
 * 전용 테이블을 두지 않는다 — 같은 사실을 두 곳에 쓰면 언젠가 갈라지고,
 * 갈라진 뒤에는 어느 쪽이 맞는지 알 수 없다.
 */
public record TenantActivityResponse(
        String id,
        Type type,
        OffsetDateTime at,
        String summary,
        String reason,
        OperatorRef operator) {

    public record OperatorRef(UUID id, String name) {
    }

    public enum Type {
        CHANGE_PLAN,
        GRANT_QUOTA,
        SUSPEND,
        CHURN,
        EXTEND_TRIAL,
        IMPERSONATE,
        VIEW_CONVERSATIONS,
        MODEL_PRICE_WRITE,
        COST_GUARD_WRITE,
        /** 시스템이 만든 항목. {@code operator} 가 null 이다. */
        PAYMENT,
        NOTE
    }
}
