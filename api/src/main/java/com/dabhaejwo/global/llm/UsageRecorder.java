package com.dabhaejwo.global.llm;

import java.math.BigDecimal;
import java.util.UUID;
import com.dabhaejwo.global.security.BotScope;

/**
 * ai_usage 적재 포트. 구현은 domain/usage 에 있다.
 * {@link LlmGateway} 외의 호출자를 만들지 않는다 — 게이트웨이를 우회하면 원가에 구멍이 난다.
 */
public interface UsageRecorder {

    void record(BotScope scope,
                UsagePurpose purpose,
                LlmProviderName provider,
                String model,
                Long modelPriceId,
                int inputTokens,
                int outputTokens,
                BigDecimal costKrw,
                UUID conversationId);
}
