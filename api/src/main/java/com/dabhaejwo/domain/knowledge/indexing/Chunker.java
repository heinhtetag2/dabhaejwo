package com.dabhaejwo.domain.knowledge.indexing;

import java.util.ArrayList;
import java.util.List;

/**
 * 글자를 조각으로 나눈다.
 *
 * <p>왜 나누는가. 문서 전체를 한 벡터로 만들면 "배송비"와 "반품 절차"가 한 점에 뭉개져
 * 어느 질문에도 어중간하게 걸린다. 조각을 작게 하면 정확도가 오르지만 맥락이 끊긴다.
 *
 * <p>두 가지로 균형을 잡는다.
 * <ul>
 *   <li><b>경계를 존중한다</b> — 문단 → 문장 순으로 자를 자리를 찾는다.
 *       글자 수만 보고 자르면 문장 한가운데가 끊겨 그 조각만으로는 뜻이 통하지 않는다</li>
 *   <li><b>겹쳐 자른다</b> — 조각 끝에 걸친 답이 어느 쪽에도 온전히 없는 상황을 막는다</li>
 * </ul>
 */
public final class Chunker {

    /**
     * 조각 목표 길이(글자).
     *
     * <p>한국어는 같은 글자 수에 영어보다 많은 정보가 담긴다. 영어권 기본값(1000자 안팎)을
     * 그대로 쓰면 한 조각이 너무 넓어진다.
     */
    static final int TARGET_CHARS = 600;
    /** 겹치는 구간. 목표의 15% 정도면 문장 하나가 통째로 잘리는 경우를 대부분 막는다. */
    static final int OVERLAP_CHARS = 90;
    /** 이보다 짧은 꼬리 조각은 만들지 않는다 — 앞 조각에 이미 겹쳐 들어가 있다. */
    static final int MIN_CHARS = 40;

    private Chunker() {
    }

    public static List<String> split(String text) {
        String source = text == null ? "" : text.strip();
        if (source.isEmpty()) {
            return List.of();
        }
        if (source.length() <= TARGET_CHARS) {
            return List.of(source);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < source.length()) {
            int hardEnd = Math.min(start + TARGET_CHARS, source.length());
            int end = hardEnd == source.length() ? hardEnd : boundaryBefore(source, start, hardEnd);

            String chunk = source.substring(start, end).strip();
            if (chunk.length() >= MIN_CHARS || chunks.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= source.length()) {
                break;
            }
            // 다음 조각은 겹치는 만큼 앞으로 당겨 시작한다.
            int next = end - OVERLAP_CHARS;
            // 경계를 못 찾아 end 가 거의 안 나아갔을 때 제자리를 도는 것을 막는다.
            start = Math.max(next, start + 1);
        }
        return chunks;
    }

    /**
     * {@code hardEnd} 이전에서 자를 자리를 찾는다. 문단 → 문장 → 공백 순으로 보고,
     * 앞쪽 절반보다 더 앞으로는 물러나지 않는다 — 너무 짧은 조각이 양산되기 때문이다.
     */
    private static int boundaryBefore(String text, int start, int hardEnd) {
        int floor = start + TARGET_CHARS / 2;

        int paragraph = text.lastIndexOf("\n\n", hardEnd);
        if (paragraph > floor) {
            return paragraph;
        }
        int sentence = lastSentenceEnd(text, floor, hardEnd);
        if (sentence > floor) {
            return sentence;
        }
        int space = text.lastIndexOf(' ', hardEnd);
        if (space > floor) {
            return space;
        }
        // 한국어는 공백 없이 길게 이어지는 경우가 있다. 그때는 글자 수로 자른다.
        return hardEnd;
    }

    /** 마침표·물음표·느낌표·줄바꿈 뒤를 문장 끝으로 본다. */
    private static int lastSentenceEnd(String text, int floor, int hardEnd) {
        for (int i = hardEnd - 1; i > floor; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '?' || c == '!' || c == '\n') {
                return i + 1;
            }
        }
        return -1;
    }
}
