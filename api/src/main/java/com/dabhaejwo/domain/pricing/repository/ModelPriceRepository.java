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
}
