package com.dabhaejwo.global.llm;

import java.util.List;

/**
 * @param vectors 입력 순서와 1:1 대응. 차원은 knowledge_chunks.embedding 과 일치해야 한다(1536).
 */
public record EmbedResult(
        List<float[]> vectors,
        String model,
        int inputTokens
) {
}
