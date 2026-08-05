package com.dabhaejwo.domain.botsettings.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 위젯을 어느 페이지에 띄울지. 틀리면 업체가 감추려던 페이지에 챗봇이 뜨거나,
 * 띄우려던 곳에 안 뜬다 — 둘 다 업체가 직접 확인하기 전까지는 모른다.
 */
class PagePatternMatcherTest {

    @Test
    @DisplayName("ALL 이면 패턴과 무관하게 전부 띄운다")
    void allIgnoresPatterns() {
        assertTrue(PagePatternMatcher.matches(PageScope.ALL, List.of("/nope"), "/anything"));
        assertTrue(PagePatternMatcher.matches(null, List.of(), "/anything"));
    }

    @Test
    @DisplayName("INCLUDE 는 맞는 경로에만 띄운다")
    void includeOnlyMatching() {
        List<String> patterns = List.of("/pricing", "/blog/*");

        assertTrue(PagePatternMatcher.matches(PageScope.INCLUDE, patterns, "/pricing"));
        assertTrue(PagePatternMatcher.matches(PageScope.INCLUDE, patterns, "/blog/2026/hello"));
        assertFalse(PagePatternMatcher.matches(PageScope.INCLUDE, patterns, "/about"));
    }

    @Test
    @DisplayName("EXCLUDE 는 맞는 경로에만 감춘다")
    void excludeHidesMatching() {
        List<String> patterns = List.of("/admin/*");

        assertFalse(PagePatternMatcher.matches(PageScope.EXCLUDE, patterns, "/admin/orders"));
        assertTrue(PagePatternMatcher.matches(PageScope.EXCLUDE, patterns, "/products"));
    }

    @Test
    @DisplayName("* 는 여러 단계를 넘는다")
    void wildcardCrossesSegments() {
        assertTrue(PagePatternMatcher.matches(
                PageScope.INCLUDE, List.of("/shop/*"), "/shop/a/b/c"));
        assertTrue(PagePatternMatcher.matches(
                PageScope.INCLUDE, List.of("*/checkout"), "/shop/cart/checkout"));
    }

    @Test
    @DisplayName("쿼리·해시는 떼고 본다")
    void ignoresQueryAndHash() {
        // 광고 유입 링크에 붙는 ?ref= 때문에 위젯이 안 뜨면 원인을 찾기 어렵다.
        assertTrue(PagePatternMatcher.matches(
                PageScope.INCLUDE, List.of("/pricing"), "/pricing?ref=ad#plans"));
    }

    @Test
    @DisplayName("INCLUDE 인데 패턴이 없으면 아무 데도 안 뜬다")
    void includeWithoutPatternsShowsNothing() {
        // "여기에만 띄운다"고 해놓고 아무것도 안 적었으면 그게 맞는 해석이다.
        assertFalse(PagePatternMatcher.matches(PageScope.INCLUDE, List.of(), "/anything"));
        assertFalse(PagePatternMatcher.matches(PageScope.INCLUDE, List.of("  "), "/anything"));
    }

    @Test
    @DisplayName("정규식 특수문자는 글자 그대로 본다")
    void treatsRegexCharsAsLiterals() {
        // 업체가 적은 것은 경로이지 정규식이 아니다. `.` 이 아무 글자나 되면 안 된다.
        assertFalse(PagePatternMatcher.matches(
                PageScope.INCLUDE, List.of("/a.c"), "/abc"));
        assertTrue(PagePatternMatcher.matches(
                PageScope.INCLUDE, List.of("/a.c"), "/a.c"));
    }

    @Test
    @DisplayName("경로가 비어 있으면 루트로 본다")
    void blankPathIsRoot() {
        assertTrue(PagePatternMatcher.matches(PageScope.INCLUDE, List.of("/"), ""));
        assertTrue(PagePatternMatcher.matches(PageScope.INCLUDE, List.of("/"), null));
    }
}
