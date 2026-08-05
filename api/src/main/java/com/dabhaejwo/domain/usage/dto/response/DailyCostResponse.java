package com.dabhaejwo.domain.usage.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 하루치 용도별 원가. 14일 누적 막대의 한 칸이다.
 *
 * <p>용도를 배열이 아니라 고정 필드로 둔 이유는 {@code purpose} 가 4종으로 닫혀 있고,
 * 누적 막대의 층 순서가 화면 계약이기 때문이다. 배열로 주면 어떤 날은 층이 빠져
 * 색이 밀린다.
 */
public record DailyCostResponse(
        LocalDate day,
        BigDecimal answerKrw,
        BigDecimal embedDocKrw,
        BigDecimal embedQueryKrw,
        BigDecimal etcKrw) {

    public BigDecimal totalKrw() {
        return answerKrw.add(embedDocKrw).add(embedQueryKrw).add(etcKrw);
    }
}
