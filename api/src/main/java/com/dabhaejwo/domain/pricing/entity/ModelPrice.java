package com.dabhaejwo.domain.pricing.entity;

import com.dabhaejwo.global.llm.LlmProviderName;
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

/**
 * 모델 단가 이력.
 *
 * <p><b>수정자가 없다.</b> 단가가 바뀌면 새 행을 추가한다.
 * 기존 행을 고치면 그 단가로 이미 계산된 과거 ai_usage 의 근거가 사라지고,
 * 언제부터 적자였는지 추적할 수 없게 된다 (admin-console-plan.md §4.7).
 */
@Entity
@Table(name = "model_prices")
public class ModelPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmProviderName provider;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose_kind", nullable = false)
    private PurposeKind purposeKind;

    @Column(name = "input_per_1m", nullable = false)
    private BigDecimal inputPer1m;

    /** 임베딩 모델은 출력 토큰이 없다. */
    @Column(name = "output_per_1m")
    private BigDecimal outputPer1m;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ModelPrice() {
    }

    public static ModelPrice register(LlmProviderName provider,
                                      String model,
                                      PurposeKind purposeKind,
                                      BigDecimal inputPer1m,
                                      BigDecimal outputPer1m,
                                      OffsetDateTime effectiveFrom,
                                      String note) {
        ModelPrice price = new ModelPrice();
        price.provider = provider;
        price.model = model;
        price.purposeKind = purposeKind;
        price.inputPer1m = inputPer1m;
        price.outputPer1m = outputPer1m;
        price.effectiveFrom = effectiveFrom;
        price.note = note;
        price.createdAt = OffsetDateTime.now();
        return price;
    }

    public Long getId() {
        return id;
    }

    public LlmProviderName getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public PurposeKind getPurposeKind() {
        return purposeKind;
    }

    public BigDecimal getInputPer1m() {
        return inputPer1m;
    }

    public BigDecimal getOutputPer1m() {
        return outputPer1m;
    }

    public OffsetDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public String getNote() {
        return note;
    }

    public enum PurposeKind {
        GENERATE,
        EMBED
    }
}
