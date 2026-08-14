package com.dabhaejwo.domain.app.dto.response;

import com.dabhaejwo.domain.bot.dto.response.BotResponse;
import com.dabhaejwo.domain.member.dto.response.MemberResponse;
import com.dabhaejwo.domain.tenant.entity.TenantStatus;

import java.util.List;
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
        /**
         * 이 업체의 서비스 전부. 셀렉터·리다이렉트 판정·설치 화면이 이걸로 산다.
         *
         * <p>따로 부르지 않고 여기 싣는 이유는 셸 부팅에 왕복이 하나 더 늘기 때문이다.
         */
        List<BotResponse> bots,
        Usage usage,
        ImpersonationContext impersonation) {

    public record TenantContext(
            UUID id,
            String name,
            String primaryDomain,
            TenantStatus status,
            PlanRef plan) {
    }

    public record PlanRef(UUID id, String name, int monthlyFee) {
    }

    /**
     * 사용량. <b>전부 업체 합산이다</b> — 계약의 단위가 업체이기 때문이다.
     *
     * <p>서비스가 여럿이면 화면이 범위를 밝혀야 한다: "이 서비스 40 / 업체 전체 248 / 한도 500".
     * 안 밝히면 "내 서비스는 40개인데 왜 한도 초과냐"가 된다.
     *
     * @param botCount 지금 만든 서비스 수. {@code botLimit} 과 짝이다
     */
    public record Usage(long convCount, int convLimit, long docCount, int docLimit,
                        long botCount, int botLimit) {
    }

    public record ImpersonationContext(UUID sessionId, String reason, String expiresAt) {
    }
}
