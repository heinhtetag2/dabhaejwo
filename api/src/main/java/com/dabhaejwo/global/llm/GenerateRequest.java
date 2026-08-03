package com.dabhaejwo.global.llm;

/**
 * @param model            공급사 모델 ID. 코드에 하드코딩하지 않고 plan_model_assignments 에서 읽는다.
 * @param systemPrompt     전 업체 공통 규칙 + 업체가 설정한 말투
 * @param userPrompt       방문자 질문 + 검색된 문서 조각
 * @param maxOutputTokens  출력 토큰 상한. 원가를 예측 가능하게 만든다.
 */
public record GenerateRequest(
        String model,
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens
) {
}
