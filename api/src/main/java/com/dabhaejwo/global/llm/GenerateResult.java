package com.dabhaejwo.global.llm;

/** 토큰 수는 공급사가 보고한 실측값이다. 추정치를 쓰면 원가가 틀어진다. */
public record GenerateResult(
        String text,
        String model,
        int inputTokens,
        int outputTokens
) {
}
