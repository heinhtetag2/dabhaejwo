package com.dabhaejwo.domain.pricing.dto.response;

import com.dabhaejwo.domain.pricing.entity.ModelPrice;
import com.dabhaejwo.global.llm.LlmProviderName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 모델 단가 한 행. api-contracts.md §7.
 *
 * @param current 그 (공급사, 모델) 조합에서 <b>지금 적용 중인 행</b>인지.
 *                서버가 계산해 준다 — 프론트가 {@code effectiveFrom} 을 비교하면
 *                미래 예약분과 현재분의 경계에서 어긋난다.
 */
public record ModelPriceResponse(
        Long id,
        LlmProviderName provider,
        String model,
        ModelPrice.PurposeKind purposeKind,
        BigDecimal inputPer1m,
        BigDecimal outputPer1m,
        OffsetDateTime effectiveFrom,
        String note,
        boolean current) {

    public static ModelPriceResponse of(ModelPrice price, boolean current) {
        return new ModelPriceResponse(
                price.getId(),
                price.getProvider(),
                price.getModel(),
                price.getPurposeKind(),
                price.getInputPer1m(),
                price.getOutputPer1m(),
                price.getEffectiveFrom(),
                price.getNote(),
                current);
    }
}
