package com.dabhaejwo.domain.tenant.dto.response;

/**
 * 필터 칩별 건수. api-contracts.md §2-5.
 *
 * <p>0인 칩도 내려준다 — 화면은 흐리게 처리하되 숨기지 않는다. 칩이 사라지면
 * "그 상태의 업체가 없다"와 "그 필터가 없다"를 구분할 수 없다
 * (admin-console-tenant-plan.md §4.1.1).
 */
public record TenantFilterCountsResponse(
        long all,
        long trial,
        long paymentFailed,
        long costExceeded,
        long inactive7d,
        long suspended,
        long churned) {
}
