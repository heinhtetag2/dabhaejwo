package com.dabhaejwo.domain.knowledge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 업로드 화이트리스트. 뚫리면 남의 서버에 실행 파일이 올라간다 —
 * 되돌릴 수 없는 종류의 사고라 테스트로 고정한다.
 */
class UploadPolicyTest {

    @Test
    @DisplayName("허용 형식만 통과한다")
    void allowsOnlyWhitelisted() {
        assertTrue(UploadPolicy.allowed("2026_카탈로그.pdf"));
        assertTrue(UploadPolicy.allowed("안내.DOCX"));
        assertTrue(UploadPolicy.allowed("가격표.xlsx"));
        assertTrue(UploadPolicy.allowed("메모.txt"));

        assertFalse(UploadPolicy.allowed("악성.exe"));
        assertFalse(UploadPolicy.allowed("script.sh"));
        assertFalse(UploadPolicy.allowed("page.html"));
        assertFalse(UploadPolicy.allowed("확장자없음"));
        assertFalse(UploadPolicy.allowed(null));
    }

    @Test
    @DisplayName("이중 확장자는 마지막 것으로 판정한다")
    void judgesByLastExtension() {
        // report.pdf.exe 는 exe 다. 앞의 pdf 에 속으면 안 된다.
        assertFalse(UploadPolicy.allowed("report.pdf.exe"));
        assertTrue(UploadPolicy.allowed("report.exe.pdf"));
    }

    @Test
    @DisplayName("경로가 섞여 와도 파일명만 본다")
    void stripsPath() {
        assertFalse(UploadPolicy.allowed("../../etc/passwd"));
        assertTrue(UploadPolicy.allowed("C:\\Users\\me\\Desktop\\카탈로그.pdf"));
        assertEquals("카탈로그.pdf",
                UploadPolicy.safeDisplayName("C:\\Users\\me\\Desktop\\카탈로그.pdf"));
        assertEquals("passwd", UploadPolicy.safeDisplayName("../../etc/passwd"));
    }

    @Test
    @DisplayName("저장할 MIME 은 확장자가 정한다 — 클라이언트 값을 그대로 쓰지 않는다")
    void contentTypeComesFromExtension() {
        assertEquals("application/pdf", UploadPolicy.contentTypeFor("a.pdf"));
        assertEquals("text/plain", UploadPolicy.contentTypeFor("a.txt"));
    }

    @Test
    @DisplayName("확장자와 어긋나는 MIME 은 거절한다")
    void rejectsConflictingContentType() {
        assertTrue(UploadPolicy.contentTypeConflicts("a.pdf", "image/png"));
        assertFalse(UploadPolicy.contentTypeConflicts("a.pdf", "application/pdf"));
        // charset 이 붙어도 앞부분만 본다
        assertFalse(UploadPolicy.contentTypeConflicts("a.txt", "text/plain; charset=utf-8"));
    }

    @Test
    @DisplayName("불특정 MIME 은 통과시킨다 — 브라우저마다 docx 를 octet-stream 으로 보낸다")
    void allowsUnspecificContentType() {
        assertFalse(UploadPolicy.contentTypeConflicts("a.docx", "application/octet-stream"));
        assertFalse(UploadPolicy.contentTypeConflicts("a.xlsx", ""));
        assertFalse(UploadPolicy.contentTypeConflicts("a.pdf", null));
    }

    @Test
    @DisplayName("제어 문자가 든 파일명은 걷어낸다")
    void stripsControlCharacters() {
        assertEquals("보고서.pdf", UploadPolicy.safeDisplayName("보고서\u0000\u001b.pdf"));
        assertEquals("이름 없는 파일", UploadPolicy.safeDisplayName("   "));
    }
}
