package com.dabhaejwo.domain.knowledge.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 분할 규칙은 검색 품질을 직접 결정한다. 조각이 문장 한가운데서 끊기면
 * 그 조각만으로는 뜻이 통하지 않아 답변 근거로 쓸 수 없다.
 */
class ChunkerTest {

    @Test
    @DisplayName("짧은 글은 통째로 하나다")
    void keepsShortTextWhole() {
        assertEquals(List.of("배송은 5~7일 걸립니다."), Chunker.split("배송은 5~7일 걸립니다."));
    }

    @Test
    @DisplayName("빈 글은 조각이 없다")
    void emptyGivesNothing() {
        assertTrue(Chunker.split("").isEmpty());
        assertTrue(Chunker.split("   ").isEmpty());
        assertTrue(Chunker.split(null).isEmpty());
    }

    @Test
    @DisplayName("긴 글은 여러 조각으로 나뉘고 전부 목표 길이 안쪽이다")
    void splitsLongText() {
        String text = ("제주 및 도서 지역도 배송 가능합니다. 지역에 따라 도선료가 추가됩니다. ").repeat(40);
        List<String> chunks = Chunker.split(text);

        assertTrue(chunks.size() > 1, "나뉘어야 한다");
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= Chunker.TARGET_CHARS,
                    "목표 길이를 넘었다: " + chunk.length());
            assertFalse(chunk.isBlank(), "빈 조각이 있다");
        }
    }

    @Test
    @DisplayName("조각이 겹쳐 경계에 걸친 문장이 사라지지 않는다")
    void chunksOverlap() {
        String text = ("가나다라마바사아자차카타파하 ").repeat(80);
        List<String> chunks = Chunker.split(text);

        // 겹침이 있으면 조각 길이 합이 원문보다 길다.
        int total = chunks.stream().mapToInt(String::length).sum();
        assertTrue(total > text.strip().length(),
                "겹치지 않았다 — 경계에 걸친 내용이 유실될 수 있다");
    }

    @Test
    @DisplayName("문단 경계에서 자른다")
    void prefersParagraphBoundary() {
        // 문단 하나가 목표 길이의 절반을 넘고 둘을 합치면 목표를 넘도록 잡는다.
        // 그래야 실제로 나뉘면서 경계 선택이 검증된다.
        String first = "배송 안내입니다. ".repeat(40);
        String second = "반품 안내입니다. ".repeat(40);
        List<String> chunks = Chunker.split(first.strip() + "\n\n" + second.strip());

        assertTrue(chunks.size() > 1, "나뉘어야 경계 선택을 볼 수 있다");
        // 첫 조각이 반품 이야기를 끌고 들어오지 않아야 한다.
        assertFalse(chunks.get(0).contains("반품"), "문단 경계를 넘어 잘렸다");
    }

    @Test
    @DisplayName("공백 없이 이어진 한국어도 무한 루프 없이 나뉜다")
    void handlesTextWithoutSpaces() {
        String text = "가".repeat(5000);
        List<String> chunks = Chunker.split(text);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.size() < 100, "조각이 지나치게 잘게 쪼개졌다: " + chunks.size());
    }

    @Test
    @DisplayName("원문 내용이 조각들 안에 모두 남는다")
    void losesNothing() {
        String text = "첫 문장입니다. 둘째 문장입니다. " + "중간 내용. ".repeat(100) + "마지막 문장입니다.";
        List<String> chunks = Chunker.split(text);

        assertTrue(chunks.get(0).startsWith("첫 문장입니다."));
        assertTrue(chunks.get(chunks.size() - 1).endsWith("마지막 문장입니다."),
                "끝부분이 잘렸다");
    }
}
