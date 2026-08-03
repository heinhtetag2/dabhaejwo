package com.dabhaejwo.domain.usage.entity;

import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.UsagePurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 모델 호출 원장. append-only — 수정자를 두지 않는다.
 *
 * <p>{@code costKrw} 는 호출 시점 단가로 확정된 값이다. 단가표가 바뀌어도 여기는 변하지 않는다.
 */
@Entity
@Table(name = "ai_usage")
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsagePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmProviderName provider;

    @Column(nullable = false)
    private String model;

    @Column(name = "model_price_id")
    private Long modelPriceId;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "cost_krw", nullable = false)
    private BigDecimal costKrw;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AiUsage() {
    }

    public static AiUsage of(UUID tenantId,
                             UsagePurpose purpose,
                             LlmProviderName provider,
                             String model,
                             Long modelPriceId,
                             int inputTokens,
                             int outputTokens,
                             BigDecimal costKrw,
                             UUID conversationId) {
        AiUsage usage = new AiUsage();
        usage.tenantId = tenantId;
        usage.purpose = purpose;
        usage.provider = provider;
        usage.model = model;
        usage.modelPriceId = modelPriceId;
        usage.inputTokens = inputTokens;
        usage.outputTokens = outputTokens;
        usage.costKrw = costKrw;
        usage.conversationId = conversationId;
        usage.createdAt = OffsetDateTime.now();
        return usage;
    }

    public Long getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UsagePurpose getPurpose() {
        return purpose;
    }

    public BigDecimal getCostKrw() {
        return costKrw;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }
}
