package com.dabhaejwo.global.llm;

import java.util.List;

/**
 * 공급사 어댑터. 공급사별 편차(엔드포인트·파라미터·응답 형상)는 구현체 밖으로 새어나가면 안 된다.
 *
 * <p><b>이 인터페이스를 서비스에서 직접 주입받아 호출하지 않는다.</b>
 * 모든 호출은 {@link LlmGateway} 를 지난다 — 그래야 ai_usage 적재를 빠뜨릴 수 없다.
 */
public interface LlmProvider {

    LlmProviderName name();

    /** 자격증명이 설정돼 있는가. false 면 게이트웨이가 이 공급사를 선택하지 않는다. */
    boolean available();

    GenerateResult generate(GenerateRequest request);

    EmbedResult embed(List<String> texts, String model);
}
