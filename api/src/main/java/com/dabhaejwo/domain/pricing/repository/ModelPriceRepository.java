package com.dabhaejwo.domain.pricing.repository;

import com.dabhaejwo.domain.pricing.entity.ModelPrice;
import com.dabhaejwo.global.llm.LlmProviderName;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ModelPriceRepository extends JpaRepository<ModelPrice, Long> {

    /** {@code at} 시점에 유효한 가장 최근 단가. */
    Optional<ModelPrice> findFirstByProviderAndModelAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            LlmProviderName provider, String model, OffsetDateTime at);

    List<ModelPrice> findAllByOrderByProviderAscModelAscEffectiveFromDesc(Limit limit);

    /**
     * 지금 쓸 임베딩 모델. <b>모델명을 코드에 두지 않기 위해</b> 단가표에서 고른다 —
     * 공급사가 모델을 갈아치우면 새 단가 행만 넣으면 되고 재배포가 필요 없다
     * (CLAUDE.md 핵심 결정).
     *
     * <p>같은 공급사에 EMBED 모델이 여럿이면 가장 최근에 유효해진 것을 쓴다.
     */
    Optional<ModelPrice> findFirstByProviderAndPurposeKindAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            LlmProviderName provider, ModelPrice.PurposeKind purposeKind, OffsetDateTime at);
}
