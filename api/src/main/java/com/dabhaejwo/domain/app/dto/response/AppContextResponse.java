package com.dabhaejwo.domain.app.dto.response;

import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;

import java.util.UUID;

/**
 * 업체 대시보드가 부팅할 때 한 번 받는 컨텍스트. api-contracts.md §9-0.
 *
 * <p>{@code tenant}·{@code usage} 의 필드명은 §2 TenantDetail 과 완전히 같다.
 * 부분집합일 뿐이며 같은 뜻의 키를 다른 이름으로 만들지 않는다.
 *
 * @param impersonation 운영자가 대리 접속 중이 아니면 null. 배너 노출 판단에 쓴다.
 */
public record AppContextResponse(
        MemberResponse member,
        TenantContext tenant,
        Usage usage,
        ImpersonationContext impersonation) {

    public record TenantContext(
            UUID id,
            String name,
            String primaryDomain,
            String publishableKey,
            TenantStatus status,
            PlanRef plan) {
    }

    public record PlanRef(UUID id, String name, int monthlyFee) {
    }

    public record Usage(long convCount, int convLimit, long docCount, int docLimit) {
    }

    public record ImpersonationContext(UUID sessionId, String reason, String expiresAt) {
    }
}
