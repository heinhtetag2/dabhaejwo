package com.dabhaejwo.domain.pricing.service;

import com.dabhaejwo.domain.pricing.entity.ModelPrice;
import com.dabhaejwo.domain.pricing.repository.ModelPriceRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.llm.LlmProviderName;
import com.dabhaejwo.global.llm.ModelPriceLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ModelPriceService implements ModelPriceLookup {

    private final ModelPriceRepository repository;

    public ModelPriceService(ModelPriceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResolvedPrice resolve(LlmProviderName provider, String model, OffsetDateTime at) {
        ModelPrice price = repository
                .findFirstByProviderAndModelAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(provider, model, at)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MODEL_PRICE_NOT_FOUND,
                        provider + "/" + model + " 의 단가가 등록되어 있지 않습니다"));
        // 단가가 없으면 조용히 0원으로 처리하지 않는다 — 원가 0인 호출이 원장에 쌓이면
        // 적자를 발견하지 못한다. 명시적으로 실패시켜 단가 등록을 강제한다.
        return new ResolvedPrice(price.getId(), price.getInputPer1m(), price.getOutputPer1m());
    }
}
