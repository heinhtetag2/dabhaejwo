package com.dabhaejwo.global.llm;

/** 공급사. 모델명과 단가는 여기가 아니라 model_prices 테이블에 있다. */
public enum LlmProviderName {
    GOOGLE,
    ANTHROPIC,
    OPENAI,
    /** 자격증명 없이 도는 로컬 stub. 응답은 가짜지만 ai_usage 적재는 진짜로 한다. */
    STUB
}
