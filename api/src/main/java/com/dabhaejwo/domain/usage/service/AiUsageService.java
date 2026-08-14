package com.dabhaejwo.domain.usage.service;

import com.dabhaejwo.domain.usage.entity.AiUsage;
import com.dabhaejwo.domain.usage.repository.AiUsageRepository;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.UsagePurpose;
import com.dabhaejwo.global.llm.UsageRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import com.dabhaejwo.global.security.BotScope;

@Service
public class AiUsageService implements UsageRecorder {

    private final AiUsageRepository repository;

    public AiUsageService(AiUsageRepository repository) {
        this.repository = repository;
    }

    /**
     * 별도 트랜잭션으로 적재한다. 호출한 비즈니스 트랜잭션이 나중에 롤백되더라도
     * 모델 호출은 이미 일어났고 비용도 이미 발생했다 — 원장에서 사라지면 안 된다.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(BotScope scope,
                       UsagePurpose purpose,
                       LlmProviderName provider,
                       String model,
                       Long modelPriceId,
                       int inputTokens,
                       int outputTokens,
                       BigDecimal costKrw,
                       UUID conversationId) {
        repository.save(AiUsage.of(scope, purpose, provider, model, modelPriceId,
                inputTokens, outputTokens, costKrw, conversationId));
    }
}
