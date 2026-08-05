package com.dabhaejwo.global.notify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "null님, 안녕하세요" 가 실제로 발송됐다. 가입 화면이 담당자 이름을 받지 않아
 * 소유자의 {@code name} 이 null 인데 그대로 찍혔다. 테스트로 고정한다.
 */
class GreetingTest {

    @Test
    @DisplayName("이름이 있으면 그대로 쓴다")
    void 이름이_있으면() {
        assertThat(Greeting.of("정민수", "min@example.com")).isEqualTo("정민수");
    }

    @Test
    @DisplayName("이름이 null 이면 이메일 앞부분 — 'null님' 이 나가지 않는다")
    void 이름이_없으면_이메일_앞부분() {
        assertThat(Greeting.of(null, "tagoplus0315@gmail.com")).isEqualTo("tagoplus0315");
        assertThat(Greeting.of("", "shop@example.com")).isEqualTo("shop");
        assertThat(Greeting.of("   ", "shop@example.com")).isEqualTo("shop");
    }

    @Test
    @DisplayName("앞뒤 공백은 지운다")
    void 공백_정리() {
        assertThat(Greeting.of("  정민수 ", "min@example.com")).isEqualTo("정민수");
    }

    @Test
    @DisplayName("둘 다 없어도 발송이 터지지 않는다")
    void 둘_다_없으면() {
        assertThat(Greeting.of(null, null)).isEqualTo("고객");
        assertThat(Greeting.of(null, "")).isEqualTo("고객");
    }

    @Test
    @DisplayName("@ 가 없는 값은 통째로 쓴다 — 잘라내려다 빈 문자열이 되는 편이 더 나쁘다")
    void 골뱅이가_없으면() {
        assertThat(Greeting.of(null, "operator")).isEqualTo("operator");
    }
}
