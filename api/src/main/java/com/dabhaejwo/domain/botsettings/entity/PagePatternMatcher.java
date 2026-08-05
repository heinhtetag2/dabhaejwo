package com.dabhaejwo.domain.botsettings.entity;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 위젯을 이 경로에 띄울지 판정한다.
 *
 * <p><b>서버가 판정한다.</b> 패턴 목록을 위젯에 내려보내 브라우저에서 맞춰보게 할 수도 있지만,
 * 그러면 업체가 어떤 페이지를 감추고 싶어 하는지가 남의 사이트 소스에 그대로 드러난다.
 * 위젯은 "띄울지 말지"라는 결론만 받는다.
 *
 * <p>패턴은 <b>{@code *} 하나만</b> 안다. 정규식을 그대로 받으면 업체가 쓸 수 없고,
 * 잘못 쓴 패턴 하나가 서버에서 폭주할 수 있다({@code (a+)+} 같은 것). 경로 매칭에
 * 필요한 것은 와일드카드 하나뿐이다.
 *
 * <pre>
 *   /pricing      → /pricing 만
 *   /blog/&#42;        → /blog/2026/hello 도, /blog/ 도 (여러 단계를 넘는다)
 *   &#42;/checkout    → /shop/checkout
 * </pre>
 */
public final class PagePatternMatcher {

    private PagePatternMatcher() {
    }

    /**
     * @param path 방문자가 보고 있는 경로. 쿼리·해시는 붙어 있어도 떼고 본다
     * @return 이 경로에 위젯을 띄워야 하면 true
     */
    public static boolean matches(PageScope scope, List<String> patterns, String path) {
        if (scope == null || scope == PageScope.ALL) {
            return true;
        }
        String normalized = normalizePath(path);
        boolean hit = patterns != null && patterns.stream()
                .filter(pattern -> pattern != null && !pattern.isBlank())
                .anyMatch(pattern -> matchesOne(pattern, normalized));

        return switch (scope) {
            case INCLUDE -> hit;
            case EXCLUDE -> !hit;
            // ALL 은 위에서 이미 걸렀다. default 를 두지 않아 값이 늘면 컴파일이 깨진다.
            case ALL -> true;
        };
    }

    /**
     * 빈 패턴 목록의 의미.
     *
     * <p>{@code INCLUDE} 인데 패턴이 없으면 아무 데도 안 뜬다 — 실수로 전부 꺼버리는 셈이라
     * 위험해 보이지만, "여기에만 띄운다"고 해놓고 아무것도 안 적었으면 그게 맞는 해석이다.
     * 화면에서 경고로 알린다.
     */
    private static boolean matchesOne(String pattern, String path) {
        return toRegex(pattern.strip()).matcher(path).matches();
    }

    private static Pattern toRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (String literal : pattern.split("\\*", -1)) {
            if (regex.length() > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(literal));
        }
        return Pattern.compile(regex.toString());
    }

    /** {@code /pricing?ref=ad#top} 과 {@code /pricing} 을 같게 본다. */
    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String value = path.strip();
        int cut = value.indexOf('?');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        cut = value.indexOf('#');
        if (cut >= 0) {
            value = value.substring(0, cut);
        }
        return value.isEmpty() ? "/" : value;
    }
}
