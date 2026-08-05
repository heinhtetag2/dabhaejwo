package com.dabhaejwo.domain.guard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 임베딩 공급사 교체.
 *
 * <p>안전장치 저장(PUT /cost-guards)과 <b>경로를 나눈 것이 의도다.</b> 이 값을 바꾸면
 * 이미 학습된 조각이 전부 무효가 된다 — 다른 모델이 만든 벡터끼리는 거리를 비교할 수 없다.
 * 슬랙 알림 토글과 같은 저장 버튼에 묶이면 사고가 난다.
 */
public record EmbeddingProviderRequest(
        @Pattern(regexp = "GOOGLE|ANTHROPIC|OPENAI|STUB") String provider,
        @NotBlank String reason) {
}
