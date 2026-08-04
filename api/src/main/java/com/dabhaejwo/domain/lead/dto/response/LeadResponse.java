package com.dabhaejwo.domain.lead.dto.response;

import com.dabhaejwo.domain.lead.entity.Lead;
import com.dabhaejwo.domain.lead.entity.LeadStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 남긴 연락처. api-contracts.md §9-3.
 *
 * @param contact <b>마스킹된 값이다.</b> 원문은 CSV 내보내기에서만 나간다 —
 *                목록 응답에 원문을 실으면 화면을 열어둔 것만으로 유출 경로가 된다
 */
public record LeadResponse(
        UUID id,
        String name,
        String contact,
        String reason,
        LeadStatus status,
        OffsetDateTime createdAt) {

    public static LeadResponse from(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.maskedContact(),
                lead.getReason(),
                lead.getStatus(),
                lead.getCreatedAt());
    }
}
