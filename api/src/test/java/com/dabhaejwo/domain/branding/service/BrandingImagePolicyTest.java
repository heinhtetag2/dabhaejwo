package com.dabhaejwo.domain.branding.service;

import com.dabhaejwo.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 업로드한 이미지는 <b>우리 도메인에서</b> 서빙되고 <b>남의 사이트</b>에 얹힌다.
 * 여기가 뚫리면 그 두 곳 모두에 남의 코드가 실린다.
 */
class BrandingImagePolicyTest {

    private static byte[] png() {
        byte[] c = new byte[16];
        c[0] = (byte) 0x89; c[1] = 'P'; c[2] = 'N'; c[3] = 'G';
        return c;
    }

    private static byte[] jpg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0};
    }

    private static byte[] webp() {
        byte[] c = new byte[16];
        byte[] head = "RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(head, 0, c, 0, head.length);
        return c;
    }

    @Test
    @DisplayName("PNG · JPG · WEBP 를 형식별로 알아본다")
    void acceptsRasterImages() {
        assertEquals("png", BrandingImagePolicy.verify(png(), "logo.png"));
        assertEquals("jpg", BrandingImagePolicy.verify(jpg(), "logo.jpg"));
        assertEquals("jpg", BrandingImagePolicy.verify(jpg(), "logo.jpeg"));
        assertEquals("webp", BrandingImagePolicy.verify(webp(), "logo.webp"));
    }

    @Test
    @DisplayName("SVG 는 거절한다 — 스크립트를 품을 수 있다")
    void rejectsSvg() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThrows(BusinessException.class, () -> BrandingImagePolicy.verify(svg, "logo.svg"));
    }

    @Test
    @DisplayName("png 로 이름 붙인 HTML 은 통과하지 못한다")
    void rejectsHtmlDisguisedAsPng() {
        // 확장자만 봤다면 통과했을 것이고, 우리 도메인에서 서빙되는 순간 XSS 가 된다.
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

        assertThrows(BusinessException.class, () -> BrandingImagePolicy.verify(html, "logo.png"));
    }

    @Test
    @DisplayName("내용과 확장자가 다르면 거절한다 — 조용히 고쳐주지 않는다")
    void rejectsMismatchedExtension() {
        assertThrows(BusinessException.class, () -> BrandingImagePolicy.verify(png(), "logo.jpg"));
    }

    @Test
    @DisplayName("512KB 를 넘으면 거절한다")
    void rejectsOversized() {
        byte[] big = new byte[BrandingImagePolicy.MAX_BYTES + 1];
        System.arraycopy(png(), 0, big, 0, 4);

        assertThrows(BusinessException.class, () -> BrandingImagePolicy.verify(big, "logo.png"));
    }

    @Test
    @DisplayName("빈 파일은 거절한다")
    void rejectsEmpty() {
        assertThrows(BusinessException.class, () -> BrandingImagePolicy.verify(new byte[0], "a.png"));
    }
}
