package com.dabhaejwo.global.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO(stub): 실제 공급사 미연동. 고정 응답 + 결정적 가짜 임베딩을 돌려준다.
 *
 * <p>중요한 것은 <b>이 stub 을 써도 ai_usage 적재가 그대로 동작한다</b>는 점이다.
 * 원가 파이프라인(단가 조회 → 원가 계산 → 원장 적재 → 일 집계 → 원가율)을
 * 공급사 연동 전에 끝까지 검증할 수 있어야 하기 때문이다.
 *
 * <p>"동작하는 척"하지 않도록 호출할 때마다 stub 임을 로그로 남긴다.
 */
@Component
public class StubLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(StubLlmProvider.class);

    /** knowledge_chunks.embedding 및 faqs.embedding 과 일치해야 한다. */
    public static final int EMBEDDING_DIMENSION = 1536;

    @Override
    public LlmProviderName name() {
        return LlmProviderName.STUB;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public GenerateResult generate(GenerateRequest request) {
        log.warn("[STUB] generate 호출 — 실제 모델을 사용하지 않았습니다. model={}", request.model());
        String text = "안내를 준비 중입니다. (stub 응답)";
        return new GenerateResult(
                text,
                request.model(),
                estimateTokens(request.systemPrompt()) + estimateTokens(request.userPrompt()),
                estimateTokens(text));
    }

    @Override
    public EmbedResult embed(List<String> texts, String model) {
        log.warn("[STUB] embed 호출 — 실제 임베딩이 아닙니다. count={} model={}", texts.size(), model);
        List<float[]> vectors = new ArrayList<>(texts.size());
        int inputTokens = 0;
        for (String text : texts) {
            vectors.add(deterministicVector(text));
            inputTokens += estimateTokens(text);
        }
        return new EmbedResult(vectors, model, inputTokens);
    }

    /**
     * 같은 텍스트는 항상 같은 벡터가 나온다. 검색 결과가 무작위로 흔들리면
     * 검색 파이프라인의 버그와 stub 의 무작위성을 구분할 수 없다.
     */
    private float[] deterministicVector(String text) {
        float[] vector = new float[EMBEDDING_DIMENSION];
        int seed = text.hashCode();
        double norm = 0.0;
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            seed = seed * 1_103_515_245 + 12_345;
            vector[i] = ((seed >>> 16) & 0x7fff) / 32767.0f - 0.5f;
            norm += (double) vector[i] * vector[i];
        }
        float length = (float) Math.sqrt(norm);
        if (length > 0) {
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                vector[i] /= length;
            }
        }
        return vector;
    }

    /** 한국어는 대략 2자당 1토큰. 실제 공급사는 실측값을 보고하므로 stub 에서만 쓴다. */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }
}
