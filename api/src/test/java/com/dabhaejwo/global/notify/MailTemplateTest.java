package com.dabhaejwo.global.notify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 메일 본문은 사용자 입력(업체명·이름)을 담는다. 이스케이프가 빠지면 본문이 깨지고
 * 링크가 가려진다 — 모델을 부르지 않고 문자열만으로 고정할 수 있으므로 테스트로 둔다.
 */
class MailTemplateTest {

    private MailTemplate.Body build(String greeting, String title) {
        return MailTemplate.build(greeting, title, "안내드립니다.",
                new MailTemplate.Highlight("인증 코드", "814259", true),
                new MailTemplate.Cta("열기", "https://example.com/invite?token=abc"),
                List.of("5분 뒤 만료됩니다."));
    }

    @Test
    @DisplayName("HTML 과 평문을 함께 만든다 — HTML 을 막는 클라이언트에서도 코드가 읽혀야 한다")
    void 두_가지_본문() {
        MailTemplate.Body body = build("민수", "로그인 인증 코드");

        assertThat(body.html()).contains("814259").contains("로그인 인증 코드");
        assertThat(body.text()).contains("814259").contains("로그인 인증 코드");
        assertThat(body.text()).doesNotContain("<div").doesNotContain("style=");
    }

    @Test
    @DisplayName("업체명에 태그를 넣어도 본문이 깨지지 않는다")
    void 사용자_입력_이스케이프() {
        MailTemplate.Body body = build("<script>alert(1)</script>", "'</td>' 가게");

        assertThat(body.html()).doesNotContain("<script>");
        assertThat(body.html()).contains("&lt;script&gt;");
        assertThat(body.html()).contains("&#39;&lt;/td&gt;&#39;");
    }

    @Test
    @DisplayName("스타일은 인라인이다 — 메일 클라이언트가 style 블록을 지운다")
    void 인라인_스타일() {
        MailTemplate.Body body = build("민수", "제목");

        assertThat(body.html()).doesNotContain("<style");
        assertThat(body.html()).contains("style=\"");
    }

    @Test
    @DisplayName("버튼이 없어도 성립한다")
    void 버튼_없음() {
        MailTemplate.Body body = MailTemplate.build(
                "민수", "제목", "본문", null, null, List.of());

        assertThat(body.html()).contains("제목");
        assertThat(body.text()).contains("제목");
    }

    @Test
    @DisplayName("링크는 버튼과 본문에 모두 넣는다 — 버튼이 안 눌리는 클라이언트가 있다")
    void 링크_두_번() {
        MailTemplate.Body body = build("민수", "초대");

        assertThat(body.html().split("https://example.com/invite").length - 1).isGreaterThanOrEqualTo(2);
        assertThat(body.text()).contains("https://example.com/invite");
    }
}
