package com.dabhaejwo.domain.gap.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 이 묶기 규칙이 "업체가 실제로 몇 번 놓쳤는가"를 결정한다.
 * 틀리면 같은 질문이 목록에 여러 줄로 쌓여 업체가 지친다.
 */
class AnswerGapTest {

    @Test
    @DisplayName("공백·문장부호 차이는 같은 질문으로 묶는다")
    void normalizesPunctuationAndSpacing() {
        assertEquals(
                AnswerGap.normalize("제주도까지 배송되나요?"),
                AnswerGap.normalize("제주도까지 배송 되나요"));
        assertEquals(
                AnswerGap.normalize("A/S 신청은 어디서 하나요"),
                AnswerGap.normalize("A/S신청은  어디서 하나요!!"));
    }

    @Test
    @DisplayName("영문 대소문자 차이도 묶는다")
    void normalizesCase() {
        assertEquals(AnswerGap.normalize("AS 접수"), AnswerGap.normalize("as 접수"));
    }

    @Test
    @DisplayName("뜻이 다른 질문은 묶지 않는다")
    void keepsDifferentQuestionsApart() {
        assertNotEquals(
                AnswerGap.normalize("배송비 얼마인가요"),
                AnswerGap.normalize("반품비 얼마인가요"));
    }

    @Test
    @DisplayName("표현만 다른 같은 뜻은 정규화로는 못 묶는다 — 임베딩 묶기가 필요한 이유")
    void normalizationCannotGroupParaphrases() {
        // tenant-plan.md §4.2 가 요구하는 묶기는 이 케이스다.
        // answer_gaps.question_embedding 을 켜면 해결된다 (docs/IMPROVEMENTS.md).
        assertNotEquals(
                AnswerGap.normalize("제주 배송되나요"),
                AnswerGap.normalize("도서지역 배송"));
    }

    @Test
    @DisplayName("넘어간 질문이 다시 들어오면 목록에 되살아난다")
    void dismissedGapRevivesOnRecurrence() {
        AnswerGap gap = AnswerGap.of(UUID.randomUUID(), "쿠폰 두 개 같이 쓸 수 있나요",
                GapReason.ANSWER_FAILED, "/cart", "확인이 어렵습니다");
        gap.dismiss();
        assertEquals(GapStatus.DISMISSED, gap.getStatus());

        gap.recur("쿠폰 두 개 같이 쓸 수 있나요", "/cart", "확인이 어렵습니다");

        assertEquals(GapStatus.OPEN, gap.getStatus());
        assertEquals(2, gap.getOccurrenceCount());
    }

    @Test
    @DisplayName("해결된 질문은 다시 물어봐도 OPEN 으로 돌아가지 않는다")
    void resolvedGapStaysResolved() {
        AnswerGap gap = AnswerGap.of(UUID.randomUUID(), "배송 며칠", GapReason.ANSWER_FAILED, "/", "몰라요");
        gap.resolveWith(UUID.randomUUID());

        gap.recur("배송 며칠", "/", "몰라요");

        // 답을 등록했는데도 또 실패했다면 그건 답변 품질 문제이지 미등록 문제가 아니다.
        assertEquals(GapStatus.RESOLVED, gap.getStatus());
    }
}
