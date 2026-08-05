package com.dabhaejwo.domain.pricing.dto.request;

import com.dabhaejwo.domain.pricing.entity.ModelPrice;
import com.dabhaejwo.global.llm.LlmProviderName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 새 단가 등록. <b>기존 행을 수정하는 요청이 아니다.</b>
 *
 * <p>{@code id} 를 받지 않는 것이 설계다. 단가는 이력이며 행을 추가만 한다 —
 * 고치면 그 단가로 이미 계산된 과거 {@code ai_usage} 의 근거가 사라진다.
 *
 * @param effectiveFrom 적용 시작 시각. 비우면 지금부터다. 미래를 넣으면 예약이 된다 —
 *                      공급사가 "다음 달부터 인상"을 공지했을 때 미리 넣어 둔다
 * @param reason        사유 필수. 단가 변경은 전체 원가 계산에 즉시 영향이 간다
 */
public record ModelPriceCreateRequest(
        @NotNull LlmProviderName provider,
        @NotBlank String model,
        @NotNull ModelPrice.PurposeKind purposeKind,
        @NotNull @DecimalMin("0.0") BigDecimal inputPer1m,
        /** 임베딩 모델은 출력 토큰이 없어 null 이다. 답변 생성 모델은 서비스가 필수로 막는다. */
        BigDecimal outputPer1m,
        OffsetDateTime effectiveFrom,
        String note,
        @NotBlank String reason) {
}
