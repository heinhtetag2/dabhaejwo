package com.dabhaejwo.domain.faq.dto.response;

import com.dabhaejwo.domain.faq.entity.Faq;

import java.util.List;
import java.util.UUID;

/** 공통 질문. api-contracts.md §9-1. */
public record FaqResponse(
        UUID id,
        String question,
        String answer,
        List<String> links,
        List<UUID> followUpFaqIds,
        boolean shown,
        int sortOrder,
        int hitCount) {

    public static FaqResponse from(Faq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getLinks(),
                faq.getFollowUpFaqIds(),
                faq.isShown(),
                faq.getSortOrder(),
                faq.getHitCount());
    }
}
