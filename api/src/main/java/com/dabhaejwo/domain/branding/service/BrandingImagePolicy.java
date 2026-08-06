package com.dabhaejwo.domain.branding.service;

import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;

import java.util.Locale;

/**
 * 브랜딩 이미지 업로드 규칙.
 *
 * <p><b>SVG 를 받지 않는다.</b> SVG 는 그림이 아니라 문서다 — 안에 {@code <script>} 를 넣을 수
 * 있고, 우리는 그것을 <b>남의 사이트 위젯</b>과 <b>우리 콘솔</b> 양쪽에 띄운다. 정화기를
 * 붙이는 방법도 있지만, 정화기의 구멍은 계속 발견된다. 래스터만 받으면 그 위험이 사라진다.
 *
 * <p>확장자와 매직 바이트를 <b>둘 다</b> 본다. 확장자만 보면 {@code .png} 로 이름 붙인 HTML 이
 * 통과하고, 그 파일이 우리 도메인에서 서빙되면 XSS 가 된다.
 */
public final class BrandingImagePolicy {

    /** 로고·아이콘은 작은 그림이다. 512KB 를 넘으면 목적이 다른 파일이라고 본다. */
    public static final int MAX_BYTES = 512 * 1024;

    private BrandingImagePolicy() {
    }

    /**
     * @return 저장에 쓸 확장자({@code png}·{@code jpg}·{@code webp})
     */
    public static String verify(byte[] content, String originalFilename) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "빈 파일입니다");
        }
        if (content.length > MAX_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "이미지는 512KB 이하여야 합니다");
        }

        String actual = sniff(content);
        if (actual == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "PNG · JPG · WEBP 이미지만 올릴 수 있습니다");
        }

        // 확장자와 실제 내용이 다르면 둘 중 하나가 거짓말이다. 내용을 믿되 거절한다 —
        // 조용히 고쳐주면 업체는 자기가 무엇을 올렸는지 모른 채 넘어간다.
        String claimed = extensionOf(originalFilename);
        if (claimed != null && !claimed.equals(actual)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "파일 형식이 확장자와 다릅니다");
        }
        return actual;
    }

    /** 매직 바이트로 실제 형식을 본다. 클라이언트가 보낸 MIME 은 보지 않는다. */
    private static String sniff(byte[] c) {
        if (c.length >= 8
                && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G') {
            return "png";
        }
        if (c.length >= 3
                && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (c.length >= 12
                && c[0] == 'R' && c[1] == 'I' && c[2] == 'F' && c[3] == 'F'
                && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return "jpeg".equals(ext) ? "jpg" : ext;
    }
}
