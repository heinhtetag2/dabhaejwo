package com.dabhaejwo.domain.knowledge.indexing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 업로드 원본에서 글자를 뽑는다.
 *
 * <p>포맷별 파서를 직접 고른다. 내용을 보고 추측하지 않고 <b>확장자로 정한다</b> —
 * 확장자는 업로드 시점에 이미 화이트리스트로 검증됐다({@code UploadPolicy}).
 */
@Component
public class TextExtractor {

    /**
     * 글자가 이보다 적으면 "글자를 뽑지 못했다"로 본다.
     *
     * <p>스캔한 PDF 는 파싱이 <b>성공</b>하고 빈 문자열이 나온다. 그대로 두면 문서가
     * 학습 완료로 남고 챗봇은 그 문서로 아무것도 못 답한다 — 업체는 이유를 영영 모른다.
     * 그래서 명시적으로 실패로 만든다 (tenant-plan.md §9 "스캔 PDF").
     */
    private static final int MIN_MEANINGFUL_CHARS = 20;

    public Extracted extract(InputStream content, String filename) throws IOException {
        String extension = extensionOf(filename);
        String text = switch (extension) {
            case "pdf" -> fromPdf(content);
            case "docx" -> fromDocx(content);
            case "xlsx" -> fromXlsx(content);
            default -> fromPlainText(content);
        };

        String normalized = normalize(text);
        if (normalized.length() < MIN_MEANINGFUL_CHARS) {
            return Extracted.empty(extension);
        }
        return new Extracted(normalized, null);
    }

    private String fromPdf(InputStream content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 페이지 순서대로 읽는다. 기본값은 내부 배치 순서라 단이 섞이는 문서가 있다.
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String fromDocx(InputStream content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(content);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 표는 셀을 탭으로, 행을 줄바꿈으로 편다.
     *
     * <p>수식은 계산 결과가 아니라 <b>마지막으로 저장된 값</b>을 읽는다. 계산하려면
     * 워크북 전체를 평가해야 하고, 외부 참조가 있으면 그마저 실패한다.
     */
    private String fromXlsx(InputStream content) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.KOREA);
        StringBuilder text = new StringBuilder();

        try (XSSFWorkbook workbook = new XSSFWorkbook(content)) {
            for (Sheet sheet : workbook) {
                text.append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (Cell cell : row) {
                        String value = formatter.formatCellValue(cell).strip();
                        if (!value.isEmpty()) {
                            if (line.length() > 0) {
                                line.append('\t');
                            }
                            line.append(value);
                        }
                    }
                    if (line.length() > 0) {
                        text.append(line).append('\n');
                    }
                }
            }
        }
        return text.toString();
    }

    /** txt·md·csv. UTF-8 로 읽는다 — 업로드는 웹에서 오고 웹의 기본은 UTF-8 이다. */
    private String fromPlainText(InputStream content) throws IOException {
        return new String(content.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 공백 정리. 임베딩 품질과 직결된다 — 줄바꿈만 다른 같은 문장이 다른 벡터가 되면
     * 검색이 흔들리고, 빈 줄이 많으면 청크 하나에 담기는 실제 내용이 줄어든다.
     */
    private String normalize(String raw) {
        return raw
                // PDF 가 자주 남기는 제어 문자와 대체 문자를 걷어낸다
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")
                .replace(' ', ' ')
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * @param text      뽑아낸 글자. {@code errorCode} 가 있으면 비어 있다
     * @param errorCode 뽑지 못한 이유. 운영자용 원문 코드이며 한글 설명은 프론트가 매핑한다
     */
    public record Extracted(String text, String errorCode) {

        static Extracted empty(String extension) {
            // 스캔 PDF 는 흔한 경우라 따로 알려준다. 업체가 "다시 올려야 한다"를 알 수 있어야 한다.
            return new Extracted("", "pdf".equals(extension) ? "pdf_no_text" : "no_text_found");
        }

        public boolean failed() {
            return errorCode != null;
        }
    }
}
