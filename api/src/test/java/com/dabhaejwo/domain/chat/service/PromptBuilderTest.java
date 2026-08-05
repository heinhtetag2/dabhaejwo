package com.dabhaejwo.domain.chat.service;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 프롬프트는 답변 품질의 뿌리다. 모델을 부르지 않고 확인할 수 있어야
 * 품질이 흔들릴 때 돈을 쓰지 않고 원인을 찾을 수 있다.
 */
class PromptBuilderTest {

    private BotSettings settings() {
        return BotSettings.defaults(UUID.randomUUID(), "노르드하임");
    }

    private KnowledgeChunkRepository.Match match(String content) {
        return new KnowledgeChunkRepository.Match(
                UUID.randomUUID(), "배송 안내", "/guide/delivery", content, 0.9);
    }

    @Test
    @DisplayName("지어내지 말라는 규칙이 항상 들어간다 — 빠지면 모르는 것을 그럴듯하게 답한다")
    void 기본_규칙() {
        String prompt = PromptBuilder.systemPrompt(settings(), null);

        assertThat(prompt).contains("절대 지어내지 않는다");
        assertThat(prompt).contains("참고 자료");
    }

    @Test
    @DisplayName("업체 설정이 프롬프트에 반영된다")
    void 업체_설정_반영() {
        BotSettings settings = settings();
        settings.editTone("정중한 존댓말", "확인이 어렵습니다", List.of("환불 규정", "법률 상담"));
        settings.editFallback(true, "1588-0000", false, null);

        String prompt = PromptBuilder.systemPrompt(settings, "전 업체 공통 지침");

        assertThat(prompt).contains("노르드하임 도우미");
        assertThat(prompt).contains("정중한 존댓말");
        assertThat(prompt).contains("환불 규정, 법률 상담");
        assertThat(prompt).contains("1588-0000");
        assertThat(prompt).contains("전 업체 공통 지침");
    }

    @Test
    @DisplayName("설정이 비어 있어도 규칙만으로 성립한다 — 기본값이 없는 업체가 있다")
    void 빈_설정() {
        BotSettings settings = settings();
        settings.editTone("", "", List.of());

        String prompt = PromptBuilder.systemPrompt(settings, "   ");

        assertThat(prompt).contains("절대 지어내지 않는다");
        assertThat(prompt).doesNotContain("말투:");
        assertThat(prompt).doesNotContain("답하지 말고");
    }

    @Test
    @DisplayName("근거가 질문보다 먼저 온다 — 뒤에 두면 긴 자료에 질문이 파묻힌다")
    void 근거가_질문보다_앞() {
        String prompt = PromptBuilder.userPrompt("환불 며칠 안에 되나요",
                List.of(match("7일 이내 환불 가능"), match("교환은 1회 무료")));

        assertThat(prompt.indexOf("7일 이내 환불 가능"))
                .isLessThan(prompt.indexOf("환불 며칠 안에 되나요"));
        assertThat(prompt).contains("[자료 1]").contains("[자료 2]");
    }

    @Test
    @DisplayName("언어 지시가 규칙과 질문 끝 양쪽에 있다 — 규칙에만 두면 영어로 답하는 일이 생긴다")
    void 언어_지시가_두_번_들어간다() {
        // 실호출에서 한국어 질문에 영어 답이 나왔다. 모델은 프롬프트 끝을 무겁게 보므로
        // 가장 자주 틀리는 지시는 마지막에 한 번 더 놓는다.
        String system = PromptBuilder.systemPrompt(settings(), null);
        String user = PromptBuilder.userPrompt("제주도 배송비가 어떻게 되나요", List.of(match("도선료 5,000원")));

        assertThat(system).contains("한국어 질문에는 한국어로만 답한다");
        assertThat(user).endsWith("위 질문과 같은 언어로 답한다.");
    }

    @Test
    @DisplayName("프롬프트에 HTML 태그가 섞이지 않는다 — 모델에 그대로 전달된다")
    void 프롬프트에_마크업이_없다() {
        String prompt = PromptBuilder.systemPrompt(settings(), null);

        assertThat(prompt).doesNotContain("<b>").doesNotContain("</b>");
    }
}
