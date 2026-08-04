package com.dabhaejwo.domain.knowledge.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 업로드 허용 규칙.
 *
 * <p><b>화이트리스트다.</b> 금지 목록으로 막으면 빠뜨린 확장자가 곧 구멍이 된다
 * (core security-rules).
 *
 * <p>확장자와 MIME 을 <b>둘 다</b> 본다. 확장자만 보면 {@code .pdf} 로 이름 붙인 실행 파일이
 * 통과하고, MIME 만 보면 클라이언트가 보낸 값을 믿는 셈이 된다. 서버가 확장자로 정한 MIME 을
 * 저장하고, 클라이언트가 보낸 값은 대조에만 쓴다.
 */
public final class UploadPolicy {

    /** 확장자 → 저장할 MIME. 챗봇이 글자를 뽑을 수 있는 형식만 둔다. */
    private static final Map<String, String> ALLOWED = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "txt", "text/plain",
            "md", "text/markdown",
            "csv", "text/csv");

    /**
     * 클라이언트가 보낸 MIME 이 이것들이면 대조를 건너뛴다.
     *
     * <p>브라우저·OS 마다 같은 파일에 다른 MIME 을 붙인다. 특히 docx·xlsx 는
     * {@code application/octet-stream} 으로 오는 경우가 흔해서, 엄격히 대조하면
     * 정상 파일이 거절된다. 확장자 화이트리스트가 이미 걸려 있으므로 여기서는 느슨해도 된다.
     */
    private static final Set<String> UNSPECIFIC = Set.of(
            "application/octet-stream", "application/download", "binary/octet-stream", "");

    private UploadPolicy() {
    }

    public static boolean allowed(String filename) {
        return ALLOWED.containsKey(extensionOf(filename));
    }

    /** 저장할 MIME. 확장자가 정한다 — 클라이언트가 보낸 값을 그대로 믿지 않는다. */
    public static String contentTypeFor(String filename) {
        return ALLOWED.get(extensionOf(filename));
    }

    /**
     * 클라이언트가 보낸 MIME 이 확장자와 어긋나는지. 어긋나면 거절한다 —
     * {@code .pdf} 인데 {@code image/png} 로 온 파일은 둘 중 하나가 거짓말이다.
     */
    public static boolean contentTypeConflicts(String filename, String declaredContentType) {
        String declared = declaredContentType == null
                ? "" : declaredContentType.toLowerCase(Locale.ROOT).split(";")[0].strip();
        if (UNSPECIFIC.contains(declared)) {
            return false;
        }
        String expected = contentTypeFor(filename);
        return expected != null && !expected.equals(declared);
    }

    public static String allowedExtensionsLabel() {
        return String.join(", ", ALLOWED.keySet().stream().sorted().toList());
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        // 경로 구분자를 먼저 걷어낸다. 브라우저에 따라 전체 경로가 오는 경우가 있다.
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);

        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 화면에 보여줄 안전한 파일명. 경로와 제어 문자를 걷어낸다. */
    public static String safeDisplayName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "이름 없는 파일";
        }
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}]", "").strip();
        return name.isEmpty() ? "이름 없는 파일" : name;
    }
}
