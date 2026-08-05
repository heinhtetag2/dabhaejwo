package com.dabhaejwo.domain.chat.service;

import com.dabhaejwo.domain.botsettings.entity.BotSettings;
import com.dabhaejwo.domain.knowledge.repository.KnowledgeChunkRepository;

import java.util.List;

/**
 * 프롬프트 조립. <b>순수 함수다</b> — DB 도 네트워크도 모른다.
 *
 * <p>답변 품질이 흔들릴 때 가장 먼저 의심할 곳이므로, 모델을 부르지 않고 문자열만
 * 확인할 수 있어야 한다. 여기가 서비스 안에 섞여 있으면 프롬프트를 보려고
 * 실제 호출을 해야 하고, 그러면 확인할 때마다 돈이 나간다.
 */
final class PromptBuilder {

    /**
     * 전 업체 공통 규칙. 업체가 못 바꾼다 — 이 문장들이 없으면 챗봇이 모르는 것을 지어낸다.
     * 업체별 문구는 {@code cost_guards.common_prompt} 와 업체 설정으로 덧붙는다.
     */
    private static final String BASE_RULES = """
            너는 아래 '참고 자료'만 근거로 방문자 질문에 답하는 고객 응대 도우미다.

            반드시 지킬 것:
            - 언어: 방문자가 질문한 언어와 같은 언어로 답한다. 한국어 질문에는 한국어로만 답한다.
              참고 자료가 다른 언어여도 답변 언어는 질문을 따른다.
            - 참고 자료에 없는 내용은 절대 지어내지 않는다. 모르면 모른다고 답한다.
            - 가격·기간·수량 같은 숫자는 참고 자료에 적힌 그대로만 말한다. 추정하지 않는다.
            - 참고 자료에 번호나 출처 표시가 있어도 답변에 그대로 옮기지 않는다.
            - 답변은 짧고 분명하게. 필요하면 줄바꿈으로 나눈다.
            """;

    /**
     * 질문 뒤에 한 번 더 붙이는 언어 지시.
     *
     * <p>규칙 목록에만 두면 <b>실제로 영어로 답하는 일이 생긴다</b>(실호출로 확인).
     * 모델은 프롬프트 끝을 무겁게 본다 — 가장 자주 틀리는 지시는 마지막에 한 번 더 놓는다.
     */
    private static final String LANGUAGE_REMINDER =
            "\n\n위 질문과 같은 언어로 답한다.";

    private PromptBuilder() {
    }

    /**
     * @param commonPrompt 운영자가 전 업체에 공통으로 얹는 지침 ({@code cost_guards.common_prompt})
     */
    static String systemPrompt(BotSettings settings, String commonPrompt) {
        StringBuilder prompt = new StringBuilder(BASE_RULES);

        if (notBlank(commonPrompt)) {
            prompt.append('\n').append(commonPrompt.strip()).append('\n');
        }
        if (notBlank(settings.getBotName())) {
            prompt.append("\n너의 이름은 '").append(settings.getBotName()).append("' 이다.");
        }
        if (notBlank(settings.getPersona())) {
            prompt.append("\n말투: ").append(settings.getPersona().strip());
        }
        if (!settings.getForbiddenTopics().isEmpty()) {
            // 금지 주제는 "답하지 마라"가 아니라 "이렇게 넘겨라"까지 적어야 한다.
            // 그냥 막으면 모델이 침묵하고, 방문자는 고장 난 줄 안다.
            prompt.append("\n다음 주제는 답하지 말고 담당자에게 문의하도록 안내한다: ")
                    .append(String.join(", ", settings.getForbiddenTopics()));
        }
        if (notBlank(settings.getSupportPhone())) {
            prompt.append("\n안내가 필요할 때 알려줄 연락처: ").append(settings.getSupportPhone());
        }
        return prompt.toString();
    }

    /**
     * 질문 + 근거.
     *
     * <p>근거를 <b>질문보다 먼저</b> 둔다. 뒤에 두면 긴 자료에 질문이 파묻혀
     * 모델이 자료를 요약하는 쪽으로 흐른다.
     */
    static String userPrompt(String question, List<KnowledgeChunkRepository.Match> evidence) {
        StringBuilder prompt = new StringBuilder("참고 자료:\n");
        for (int i = 0; i < evidence.size(); i++) {
            prompt.append("[자료 ").append(i + 1).append("] ")
                    .append(evidence.get(i).content().strip()).append("\n\n");
        }
        return prompt.append("방문자 질문: ").append(question.strip())
                .append(LANGUAGE_REMINDER)
                .toString();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
