package com.dabhaejwo.global.common;

import java.util.Locale;

/**
 * 호스트명 정규화.
 *
 * <p>저장 형태와 비교 형태가 어긋나면 <b>위젯 호출이 전부 403 이 된다.</b> 그래서 규칙을
 * 한 곳에만 둔다 — 허용 주소와 서비스 대표 도메인이 같은 규칙을 써야 하는데, 두 도메인이
 * 각자 구현하면 언젠가 갈린다.
 */
public final class HostName {

    private HostName() {
    }

    /** {@code https://shop.example.com:443/path} 와 {@code shop.example.com} 을 같게 본다. */
    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
        value = value.replaceFirst("^https?://", "");
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        int colon = value.indexOf(':');
        if (colon >= 0) {
            value = value.substring(0, colon);
        }
        return value;
    }
}
