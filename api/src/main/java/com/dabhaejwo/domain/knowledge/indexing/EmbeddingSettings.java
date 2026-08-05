package com.dabhaejwo.domain.knowledge.indexing;

import com.dabhaejwo.domain.guard.repository.CostGuardRepository;
import com.dabhaejwo.domain.pricing.entity.ModelPrice;
import com.dabhaejwo.domain.pricing.repository.ModelPriceRepository;
import com.dabhaejwo.global.exception.BusinessException;
import com.dabhaejwo.global.exception.ErrorCode;
import com.dabhaejwo.global.llm.LlmProviderName;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 임베딩에 쓸 공급사와 모델. <b>문서 학습과 질문 검색이 반드시 같은 값을 봐야 한다.</b>
 *
 * <p>한 곳으로 모은 이유는 실제로 갈라졌기 때문이다 — 문서 학습은 설정값
 * ({@code cost_guards.embedding_provider})으로 옮겼는데 질문 검색은 환경변수를 계속 보고 있었다.
 * 그 결과 문서는 Gemini 벡터, 질문은 stub 해시 벡터가 되어 <b>검색이 조용히 아무것도
 * 찾지 못했다.</b> 오류도 로그도 없이 "답변 실패"로만 보이는 종류의 고장이다.
 *
 * <p>모델명은 코드에 두지 않고 단가표에서 고른다. 공급사가 모델을 갈아치우면
 * 새 단가 행만 넣으면 되고 재배포가 필요 없다.
 */
@Component
public class EmbeddingSettings {

    private final CostGuardRepository costGuardRepository;
    private final ModelPriceRepository modelPriceRepository;

    public EmbeddingSettings(CostGuardRepository costGuardRepository,
                             ModelPriceRepository modelPriceRepository) {
        this.costGuardRepository = costGuardRepository;
        this.modelPriceRepository = modelPriceRepository;
    }

    @Transactional(readOnly = true)
    public LlmProviderName provider() {
        return LlmProviderName.valueOf(costGuardRepository.current().getEmbeddingProvider());
    }

    @Transactional(readOnly = true)
    public String model(LlmProviderName provider) {
        return modelPriceRepository
                .findFirstByProviderAndPurposeKindAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        provider, ModelPrice.PurposeKind.EMBED, OffsetDateTime.now())
                .map(ModelPrice::getModel)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEATURE_NOT_READY,
                        provider + " 공급사의 임베딩 모델 단가가 등록되어 있지 않습니다"));
    }
}
