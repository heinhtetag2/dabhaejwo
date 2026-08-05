package com.dabhaejwo.global.notify;

import java.util.List;

/**
 * 메일 본문 조립.
 *
 * <p>HTML 과 평문을 <b>함께</b> 만든다. 메일 클라이언트가 HTML 을 막아도 내용이 읽혀야 하고,
 * 그러지 않으면 인증 코드를 못 받는 사람이 생긴다.
 *
 * <p>사용자 입력(업체명·이름)이 본문에 들어가므로 <b>HTML 로 넣을 때는 반드시 이스케이프한다.</b>
 * 업체명에 {@code <script>} 를 넣는 것은 누구나 할 수 있고, 메일 클라이언트가 그걸 실행하지는
 * 않더라도 본문이 깨지고 링크가 가려진다.
 *
 * <p>스타일은 전부 인라인이다 — 메일 클라이언트는 {@code <style>} 블록을 지우는 일이 흔하다.
 * 웹폰트도 쓰지 않는다. 표(table) 기반 레이아웃을 쓰는 것도 같은 이유다.
 */
public final class MailTemplate {

    /** 브랜드 색. 화면의 {@code --color-ink} · {@code --color-mark} 와 같은 값이다. */
    private static final String INK = "#17222e";
    private static final String MARK = "#f2b705";
    private static final String SLATE = "#5c6e7e";
    private static final String LINE = "#e7ebea";
    private static final String FILL = "#f4f6f5";

    private MailTemplate() {
    }

    /**
     * @param title    큰 제목
     * @param intro    제목 아래 한두 문장
     * @param block    강조 블록(인증 코드·임시 비밀번호). 없으면 {@code null}
     * @param cta      버튼. 없으면 {@code null}
     * @param notes    아래에 붙는 안내 문장들
     */
    public static Body build(String greeting, String title, String intro,
                             Highlight block, Cta cta, List<String> notes) {
        return new Body(html(greeting, title, intro, block, cta, notes),
                text(greeting, title, intro, block, cta, notes));
    }

    private static String html(String greeting, String title, String intro,
                               Highlight block, Cta cta, List<String> notes) {
        StringBuilder out = new StringBuilder();
        out.append("""
                <div style="margin:0;padding:24px 12px;background:%s;">
                  <table role="presentation" cellpadding="0" cellspacing="0" border="0"
                         style="width:100%%;max-width:520px;margin:0 auto;background:#ffffff;
                                border-radius:16px;border:1px solid %s;">
                    <tr><td style="padding:32px 32px 0 32px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                          <td style="width:30px;height:30px;background:%s;border-radius:9px;
                                     text-align:center;vertical-align:middle;color:%s;
                                     font-size:14px;font-weight:700;">답</td>
                          <td style="padding-left:9px;font-size:16px;font-weight:700;color:%s;
                                     letter-spacing:-0.02em;">답해줘</td>
                        </tr>
                      </table>
                    </td></tr>
                """.formatted(FILL, LINE, INK, MARK, INK));

        out.append("""
                    <tr><td style="padding:26px 32px 0 32px;">
                      <div style="font-size:22px;font-weight:700;color:%s;letter-spacing:-0.03em;
                                  line-height:1.4;">%s</div>
                      <div style="margin-top:14px;font-size:14.5px;line-height:1.75;color:%s;">
                        %s님, %s
                      </div>
                    </td></tr>
                """.formatted(INK, escape(title), SLATE, escape(greeting), escape(intro)));

        if (block != null) {
            out.append("""
                        <tr><td style="padding:22px 32px 0 32px;">
                          <div style="background:%s;border-radius:12px;padding:20px;text-align:center;">
                            <div style="font-size:12px;color:%s;">%s</div>
                            <div style="margin-top:8px;font-size:28px;font-weight:700;color:%s;
                                        letter-spacing:%s;font-family:'Courier New',monospace;
                                        word-break:break-all;">%s</div>
                          </div>
                        </td></tr>
                    """.formatted(FILL, SLATE, escape(block.label()), INK,
                    block.wideLetters() ? "0.18em" : "0", escape(block.value())));
        }

        if (cta != null) {
            out.append("""
                        <tr><td style="padding:24px 32px 0 32px;" align="center">
                          <a href="%s" style="display:inline-block;background:%s;color:#ffffff;
                             text-decoration:none;font-size:15px;font-weight:600;
                             padding:14px 28px;border-radius:12px;">%s</a>
                          <div style="margin-top:14px;font-size:11.5px;color:%s;line-height:1.7;
                                      word-break:break-all;">
                            버튼이 눌리지 않으면 아래 주소를 복사해 붙여넣어 주세요.<br>%s
                          </div>
                        </td></tr>
                    """.formatted(escape(cta.url()), INK, escape(cta.label()), SLATE, escape(cta.url())));
        }

        if (!notes.isEmpty()) {
            out.append("""
                        <tr><td style="padding:24px 32px 0 32px;">
                          <div style="border-top:1px solid %s;padding-top:18px;font-size:12.5px;
                                      line-height:1.8;color:%s;">%s</div>
                        </td></tr>
                    """.formatted(LINE, SLATE,
                    String.join("<br>", notes.stream().map(MailTemplate::escape).toList())));
        }

        out.append("""
                    <tr><td style="padding:26px 32px 30px 32px;">
                      <div style="font-size:11.5px;color:%s;">답해줘 드림</div>
                    </td></tr>
                  </table>
                </div>
                """.formatted(SLATE));
        return out.toString();
    }

    private static String text(String greeting, String title, String intro,
                               Highlight block, Cta cta, List<String> notes) {
        StringBuilder out = new StringBuilder();
        out.append(title).append("\n\n");
        out.append(greeting).append("님, ").append(intro).append("\n");
        if (block != null) {
            out.append("\n").append(block.label()).append("\n\n    ").append(block.value()).append("\n");
        }
        if (cta != null) {
            out.append("\n").append(cta.label()).append("\n\n    ").append(cta.url()).append("\n");
        }
        if (!notes.isEmpty()) {
            out.append("\n").append(String.join("\n", notes)).append("\n");
        }
        return out.append("\n답해줘 드림\n").toString();
    }

    /**
     * HTML 이스케이프. 업체명·이름은 사용자가 정하므로 그대로 넣으면 본문이 깨진다.
     * 링크(URL)도 통과시킨다 — 우리가 만든 값이지만 예외를 두면 언젠가 안 거치는 경로가 생긴다.
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** @param wideLetters 인증 코드처럼 한 글자씩 읽어야 하면 자간을 벌린다 */
    public record Highlight(String label, String value, boolean wideLetters) {
    }

    public record Cta(String label, String url) {
    }

    public record Body(String html, String text) {
    }
}
