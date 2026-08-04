package com.dabhaejwo.domain.gap.dto.response;

import com.dabhaejwo.domain.gap.entity.AnswerGap;
import com.dabhaejwo.domain.gap.entity.GapReason;
import com.dabhaejwo.domain.gap.entity.GapStatus;

import java.time.OffsetDateTime;

/** 답변 개선 대상. api-contracts.md §9-3. */
public record AnswerGapResponse(
        Long id,
        String question,
        GapReason reason,
        int occurrenceCount,
        OffsetDateTime lastAskedAt,
        String lastPath,
        String botAnswer,
        GapStatus status) {

    public static AnswerGapResponse from(AnswerGap gap) {
        return new AnswerGapResponse(
                gap.getId(),
                gap.getQuestion(),
                gap.getReason(),
                gap.getOccurrenceCount(),
                gap.getLastAskedAt(),
                gap.getLastPath(),
                gap.getBotAnswer(),
                gap.getStatus());
    }
}
